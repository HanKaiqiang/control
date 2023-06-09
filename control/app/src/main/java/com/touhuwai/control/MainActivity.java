package com.touhuwai.control;

import static com.touhuwai.control.db.DbHelper.DEFAULT_TABLE;
import static com.touhuwai.control.db.DbHelper.DELETE_DEFAULT_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.DELETE_FILE_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.FILE_DOWN_STATUS_ERROR;
import static com.touhuwai.control.db.DbHelper.FILE_DOWN_STATUS_SUCCESS;
import static com.touhuwai.control.db.DbHelper.MQTT_TABLE;
import static com.touhuwai.control.db.DbHelper.SELECT_DEFAULT_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.SELECT_FILE_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.SELECT_MQTT_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.UPDATE_FILE_OCCUPIED_SQL;
import static com.touhuwai.control.db.DbHelper.UPDATE_FILE_UNOCCUPIED_SQL;
import static com.touhuwai.control.utils.FileUtils.DEFAULT_DURATION;
import static com.touhuwai.control.utils.FileUtils.TYPE_GIF;
import static com.touhuwai.control.utils.FileUtils.TYPE_IMAGE;
import static com.touhuwai.control.utils.FileUtils.TYPE_MAP;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.touhuwai.control.db.DbHelper;
import com.touhuwai.control.utils.DeviceInfoUtil;
import com.touhuwai.control.utils.FileUtils;
import com.touhuwai.hiadvbox.HiAdvBox;
import com.touhuwai.hiadvbox.HiAdvItem;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private SQLiteDatabase db;
    private HiAdvBox hi_adv_box;
    private MqttAsyncClient mqttClient;
    private MqttClientPersistence persistence = new MemoryPersistence();
    public static final String TOPIC = "touhuwai/player/", TOPIC_DEFAULT = "touhuwai/player/default/";
    private String fileDir, deviceId, defaultFileDir;
    private EditText mServerIpEditText, mUsernameEditText, mPasswordEditText;
    private Button mConnectButton;
    private static final int REQUEST_READ_PHONE_STATE = 1;
    private static final int DEFAULT_RETRY_COUNT = 3;

    private static int DELAY_TIME = 1000; // 每秒检测是否需要播垫播素材
    private int mTimeRemaining;
    private ProgressBar progressBar;
    private Handler mTimerHandler = new Handler();
    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTimeRemaining == 0) {
                List<HiAdvItem> dataList = new ArrayList<>();
                Cursor cursor = db.rawQuery(SELECT_DEFAULT_TABLE_SQL, null);
                int count = cursor.getCount();
                if (count != 0) {
                    cursor.moveToFirst();  //移动到首位
                    for (int i = 0; i < cursor.getCount(); i++) {
                        String url = cursor.getString(1);
                        String filePath = cursor.getString(2);
                        String type = cursor.getString(3);
                        Integer duration = cursor.getInt(4);
                        dataList.add(new HiAdvItem(TYPE_MAP.get(type), duration, url, String.valueOf(filePath)));
                        //移动到下一位
                        cursor.moveToNext();
                    }
                }
                MainActivity.this.runOnUiThread(() -> hi_adv_box.restartWork(dataList));
            } else {
                mTimeRemaining --;
                mTimerHandler.postDelayed(this, DELAY_TIME);
            }
        }
    };

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_READ_PHONE_STATE);
        }
        fileDir = FileUtils.getFilePath(this.getApplicationContext(), "");
        defaultFileDir = FileUtils.getFilePath(this.getApplicationContext(), "default/");
        deviceId = DeviceInfoUtil.getDeviceId(this.getApplicationContext());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fullScreen();
        DbHelper dbHelper = new DbHelper(getApplicationContext());
        db = dbHelper.getWritableDatabase();
        requestPermissions();
        showView();
    }

    private int progress = 0;

    private void showView () {
        if (!checkMqttServer()) {
            setContentView(R.layout.activity_login);
            // 获取 TextView 对象并初始化 Handler
            TextView deviceInfoTextView = findViewById(R.id.device_info_text_view);
            String text = "DeviceId：" + deviceId + "\n" +
                    "DeviceIp：" + DeviceInfoUtil.getDeviceIpAddress(this.getApplicationContext()) + "\n" +
                    "Status：" + "offLine";
            if (!networkIsConnect()) {
                text = text + "\n" +
                        "Unable to connect to the internet";
            }
            deviceInfoTextView.setText(text);
            mServerIpEditText = findViewById(R.id.server_ip);
            mUsernameEditText = findViewById(R.id.username);
            mPasswordEditText = findViewById(R.id.password);
            mConnectButton = findViewById(R.id.connect);
            mConnectButton.setOnClickListener(view -> {
                String serverIp = mServerIpEditText.getText().toString().trim();
                serverIp = "tcp://" + serverIp + ":1883";
                String username = mUsernameEditText.getText().toString().trim();
                String password = mPasswordEditText.getText().toString().trim();
                boolean mqttConnect = mqtt(serverIp, username, password);
                if(mqttConnect){
                    ContentValues cValue = new ContentValues();
                    cValue.put("server_ip", serverIp);
                    cValue.put("username", username);
                    cValue.put("password", password);
                    db.insert(MQTT_TABLE, null, cValue);
                    Toast.makeText(MainActivity.this, "Connect Success", Toast.LENGTH_SHORT).show();
                    connectSuccess();
                }else{
                    Toast.makeText(MainActivity.this, "Connection failed, serverIp/username/password error", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            connectSuccess();
        }
    }

    private void connectSuccess () {
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setProgress(progress);
        hi_adv_box = findViewById(R.id.hi_adv_box);
        hi_adv_box.init(this, db);
    }

    private long lastMessageTime;

    private boolean mqtt (String SERVER_URI, String USERNAME, String PASSWORD) {
        try {
            mqttClient = new MqttAsyncClient(SERVER_URI, deviceId, persistence);
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    try {
                        mqttClient.subscribe(TOPIC + deviceId, 2).waitForCompletion();
                        mqttClient.subscribe(TOPIC_DEFAULT + deviceId, 2).waitForCompletion();
                    } catch (MqttException e) {
                        Log.e("MainActivity", e.getMessage(), e);
                    }
                }
                @Override
                public void connectionLost(Throwable cause) {
                }
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    try {
                        String payload = new String(message.getPayload());
                        List<HiAdvItem> dataList = new ArrayList<>();
                        if ("".equals(payload)) {
//                            dataList.add(new Advance(R.drawable.img, 0));
                        } else {
                            long currentTimeMillis = System.currentTimeMillis();
                            lastMessageTime = currentTimeMillis;
                            JSONObject jsonObject = new JSONObject(payload);
                            JSONArray playList = jsonObject.getJSONArray("playList");
                            if (topic.startsWith(TOPIC_DEFAULT)) {
                                // 垫播列表，收到消息后先下载至本地，后续由监听器切换
                                messageToAdvance(playList, true, currentTimeMillis);
                            } else {
                                dataList = messageToAdvance(playList, false, currentTimeMillis);
                                if (!jsonObject.isNull("totalDuration")) {
                                    int totalDuration = jsonObject.getInt("totalDuration");
                                    mTimeRemaining = totalDuration;
                                    mTimerHandler.postDelayed(mTimerRunnable, DELAY_TIME);
                                }
                            }
                            List<HiAdvItem> finalDataList = dataList;
                            if (currentTimeMillis == lastMessageTime) {
                                progressBar.setVisibility(View.INVISIBLE);
                                hi_adv_box.progress = progress;
                                MainActivity.this.runOnUiThread(() -> hi_adv_box.restartWork(finalDataList));
                            }
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", e.getMessage(), e);
                        publish(TOPIC + "error", e.getMessage().getBytes(StandardCharsets.UTF_8));
                    }
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(USERNAME);
            options.setPassword(PASSWORD.toCharArray());
            options.setAutomaticReconnect(true);
            mqttClient.connect(options, null, null).waitForCompletion();
            return true;
        } catch (MqttException e) {
            Log.e("MainActivity", e.getMessage(), e);
            return false;
        }
    }

    public void publish(String topic, byte[] payload) {
        try {
            MqttMessage message = new MqttMessage(payload);
            mqttClient.publish(topic, message).waitForCompletion();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private boolean checkMqttServer () {
        Cursor mqttServer = db.rawQuery(SELECT_MQTT_TABLE_SQL, null);
        if (mqttServer.getCount() == 0) {
            return false;
        }
        mqttServer.moveToFirst();
        //  "tcp://192.168.5.120:1883"
        String SERVER_URI = mqttServer.getString(1).trim();
        String USERNAME = mqttServer.getString(2).trim();
        String PASSWORD = mqttServer.getString(3).trim();
        boolean mqttConnect = mqtt(SERVER_URI, USERNAME, PASSWORD);
        if (mqttConnect) {
            return true;
        } else {
            String sql = "delete from " + MQTT_TABLE;
            db.execSQL(sql);
            return false;
        }
    }


    private List<HiAdvItem> messageToAdvance (JSONArray playList, boolean isDefaultPlay, long currentTimeMillis) throws Exception {
        List<HiAdvItem> dataList = new ArrayList<>();
        List<Object> currentPlayList = new ArrayList<>();
        for (int i = 0; i < playList.length(); i++) {
            Double v = (i+ 1) * 1.0 / playList.length() * 100;
            progress = v.intValue();
            progressBar.setProgress(progress);
            hi_adv_box.progress = progress;
            if (currentTimeMillis < lastMessageTime && !isDefaultPlay) {
                // 接收到新消息，前置消息不再下载
                return dataList;
            }
            JSONObject item = playList.getJSONObject(i);
            String fileUrl = item.getString("url");
            String type = item.getString("type");
            Integer duration = null;
            if (item.isNull("duration")) {
                if (TYPE_IMAGE.equals(type)) { // 图片播放时长为空时，设置默认时长5S
                    duration = DEFAULT_DURATION;
                }
            } else {
                duration = item.getInt("duration");
            }
            if (isDefaultPlay) {
                downDefaultPlay(fileUrl, type, duration, i == 0);
                continue;
            }
            Object filePath;
            if (TYPE_GIF.equals(type)) {
                filePath = fileUrl;
            } else {
                Map<String, Object> fileInfo = queryFilePath(fileUrl, false, DEFAULT_RETRY_COUNT);
                filePath = fileInfo.get("filePath");
                Object id = fileInfo.get("id");
                if (id != null) {
                    currentPlayList.add(id);
                }
                if (filePath instanceof Integer) {
                    type = TYPE_IMAGE;
                }
            }

            // todo error文件
            dataList.add(new HiAdvItem(TYPE_MAP.get(type), duration, fileUrl, String.valueOf(filePath)));
        }
        if (!isDefaultPlay && !currentPlayList.isEmpty()) {
            StringBuilder where = new StringBuilder(" in (");
            for (int i = 0; i < currentPlayList.size(); i++) {
                Object id = currentPlayList.get(i);
                where.append(id);
                if (i != currentPlayList.size() -1) {
                    where.append(", ");
                }
            }
            where.append(") ");
            db.execSQL(UPDATE_FILE_UNOCCUPIED_SQL + "and id not" + where);
            db.execSQL(UPDATE_FILE_OCCUPIED_SQL + "and id " + where);
        }
        return dataList;
    }

    private void downDefaultPlay (String fileUrl, String type, Integer duration, boolean isDelete) {
        Cursor cursor = db.rawQuery(SELECT_DEFAULT_TABLE_SQL,null);
        int count = cursor.getCount();
        if (count != 0 && isDelete) { // 删除现有垫播信息
            cursor.moveToFirst();  //移动到首位
            for (int i = 0; i < cursor.getCount(); i++) {
                String path = cursor.getString(2);
                FileUtils.deleteTempFile(new File(path), DEFAULT_RETRY_COUNT);
                //移动到下一位
                cursor.moveToNext();
            }
            db.execSQL(DELETE_DEFAULT_TABLE_SQL);
        }
        ContentValues cValue = new ContentValues();
        cValue.put("url", fileUrl);
        try {
            String path = FileUtils.downFile(fileUrl, defaultFileDir, db);
            cValue.put("path", path);
            cValue.put("type", type);
            cValue.put("duration", duration);
            cValue.put("status", FILE_DOWN_STATUS_SUCCESS);
        } catch (Exception e) {
            cValue.put("status", FILE_DOWN_STATUS_ERROR);
            Log.e("MainActivity", e.getMessage(), e);
        } finally {
            db.insert(DEFAULT_TABLE, null, cValue);
            Log.e("FileUtils", fileUrl + "文件下载结束");
        }

    }


    private Map<String, Object> queryFilePath (String imageUrl, boolean isForceDown, int retryCount) {
        int img = R.drawable.img;
        Map<String, Object> fileInfo = new HashMap<>();
        Object filePath = img;
        Object id = null;
        try {
            String sql = SELECT_FILE_TABLE_SQL + " and url = '" + imageUrl + "'";
            Cursor cursor = db.rawQuery(sql,null);
            if (cursor.getCount() != 0 && !isForceDown) { // 数据库中查询到数据, 使用缓存
                Map<String, Object> result = _path(cursor, retryCount);
                id = result.get("id");
                filePath = result.get("filePath");
                Object status = result.get("status");
                if (status  == FILE_DOWN_STATUS_ERROR) {
                    db.execSQL(DELETE_FILE_TABLE_SQL + " and id = " + id);
                   return queryFilePath(imageUrl, true, retryCount);
                }
            } else {
                return FileUtils.downloadAndSaveFile(imageUrl, fileDir, db);
            }
        } catch (Exception e) {
            Log.e("MainActivity", e.getMessage(), e);
        }
        fileInfo.put("filePath", filePath);
        fileInfo.put("id", id);
        return fileInfo;
    }


    private Map<String, Object> _path (Cursor dbFiles, int retryCount) {
        dbFiles.moveToFirst();
        Integer status = dbFiles.getInt(3);
        String url = dbFiles.getString(1);
        Object filePath = R.drawable.img;
        if (status == FILE_DOWN_STATUS_SUCCESS) {
            filePath = dbFiles.getString(2);
        } else {
            // 重试
            if (retryCount > 0) {
                retryCount--;
                int finalRetryCount = retryCount;
                CompletableFuture.runAsync(() -> {
                    queryFilePath(url, true, finalRetryCount);
                });
            }
            filePath = R.drawable.img;
        }
        Integer id = dbFiles.getInt(0);
        dbFiles.close();
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("filePath", filePath);
        result.put("status", status);
        return result;
    }


    private void fullScreen() {
        // 隐藏状态栏、标题栏
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 横屏
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // 设置全屏模式
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 断开MQTT连接
        try {
            mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public boolean networkIsConnect () {
        // 获取 ConnectivityManager 对象
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // 获取当前默认网络的信息，可能为空(null)
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        // 判断网络状态是否可用
        return activeNetwork != null && activeNetwork.isConnected();
    }

}