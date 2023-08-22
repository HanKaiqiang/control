package com.touhuwai.control;

import static com.touhuwai.control.db.DbHelper.DEFAULT_TABLE;
import static com.touhuwai.control.db.DbHelper.DELETE_DEFAULT_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.DELETE_FILE_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.FILE_DOWN_STATUS_ERROR;
import static com.touhuwai.control.db.DbHelper.FILE_DOWN_STATUS_SUCCESS;
import static com.touhuwai.control.db.DbHelper.FILE_OCCUPY;
import static com.touhuwai.control.db.DbHelper.MQTT_TABLE;
import static com.touhuwai.control.db.DbHelper.SELECT_DEFAULT_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.SELECT_FILE_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.SELECT_MQTT_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.UPDATE_DEFAULT_TABLE_NOCCUPIED_SQL;
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
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.touhuwai.control.db.DbHelper;
import com.touhuwai.control.entry.Event;
import com.touhuwai.control.entry.ServerDto;
import com.touhuwai.control.utils.DeviceInfoUtil;
import com.touhuwai.control.utils.FileUtils;
import com.touhuwai.control.utils.RandomNumberUtil;
import com.touhuwai.hiadvbox.HiAdvBox;
import com.touhuwai.hiadvbox.HiAdvItem;
import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private boolean isPlayDefault = true;
    private int qos = 0;

    private TextView wifiTextView;

    private Server mServer;
    private Handler mTimerHandler = new Handler();
    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTimeRemaining == 0) {
                List<HiAdvItem> dataList = new ArrayList<>();
                Cursor cursor = db.rawQuery(SELECT_DEFAULT_TABLE_SQL + " and occupy = 1 ", null);
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
                isPlayDefault = true;
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
//        LogToFile.createLogFile(this.getApplicationContext());
//        LogToFile.writeLogToFile();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        serverInit();
        DbHelper dbHelper = new DbHelper(getApplicationContext());
        db = dbHelper.getWritableDatabase();
        requestPermissions();
        FileUtils.handleSSLHandshake();
        setContentView(R.layout.activity_loading);
        showView(false);
        wifiRssi();
    }

    private int progress = 0;

    private void showView (boolean isBack) {
        if (isBack || !checkMqttServer()) {
            setContentView(R.layout.activity_login);
            TextView deviceInfoTextView = findViewById(R.id.device_info_text_view);
            String text = "DeviceId：" + deviceId + "\n" +
                    "DeviceIp：" + DeviceInfoUtil.getDeviceIpAddress(this.getApplicationContext()) + "\n" +
                    "Status：" + "offLine";
            if (!networkIsConnect()) {
                text = text + "\n" +
                        "Unable to connect";
            }
            deviceInfoTextView.setText(text);
            mServerIpEditText = findViewById(R.id.server_ip);
            mUsernameEditText = findViewById(R.id.username);
            mPasswordEditText = findViewById(R.id.password);
            mConnectButton = findViewById(R.id.connect);
            ServerDto serverInfo = getServerInfo();
            if (serverInfo != null) {
                mServerIpEditText.setText(serverInfo.url.replace("tcp://", "").replace(":1883", ""));
                mUsernameEditText.setText(serverInfo.userName);
                mPasswordEditText.setText(serverInfo.password);
                if (!isBack) {
                    mqttConnectHandler.postDelayed(mqttReConnectRunnable, 5);
                }
            }
            mConnectButton.setOnClickListener(view -> {
                String serverIp = mServerIpEditText.getText().toString().trim();
                String username = mUsernameEditText.getText().toString().trim();
                String password = mPasswordEditText.getText().toString().trim();
                if (serverIp.length() == 0 || username.length() == 0 || password.length() == 0) {
                    Toast.makeText(MainActivity.this, "error, serverIp/username/password cannot be empty", Toast.LENGTH_LONG).show();
                    return;
                }
                if (serverIp.contains(":")) {
                    serverIp = "tcp://" + serverIp;
                } else {
                    serverIp = "tcp://" + serverIp + ":1883";
                }
                boolean mqttConnect = mqtt(serverIp, username, password);
                if (mqttConnect) {
                    ContentValues cValue = new ContentValues();
                    cValue.put("server_ip", serverIp);
                    cValue.put("username", username);
                    cValue.put("password", password);
                    db.insert(MQTT_TABLE, null, cValue);
                    Toast.makeText(MainActivity.this, "Connect Success", Toast.LENGTH_LONG).show();
                    connectSuccess();
                } else {
                    Toast.makeText(MainActivity.this, "Connection failed, serverIp/username/password error", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            connectSuccess();
        }
    }

    private void connectSuccess () {
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progress);
        progressBar.setProgress(progress);
        hi_adv_box = findViewById(R.id.hi_adv_box);
        hi_adv_box.init(this, db);
    }

    private long lastMessageTime;
    private static MqttConnectOptions options = new MqttConnectOptions();
    static {
        options.setAutomaticReconnect(true);
        // 设置超时时间 单位为秒
        options.setConnectionTimeout(10);
        // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
        options.setKeepAliveInterval(20);
    }

    private boolean mqtt (String SERVER_URI, String USERNAME, String PASSWORD) {
        try {
            mqttClient = new MqttAsyncClient(SERVER_URI, deviceId, persistence);
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    try {
                        String randomString = RandomNumberUtil.getRandomString(10);
                        mqttClient.subscribe(TOPIC + randomString + deviceId, qos).waitForCompletion();
                        mqttClient.subscribe(TOPIC_DEFAULT + randomString + deviceId, qos).waitForCompletion();
                    } catch (MqttException e) {
                        Log.e("MainActivity", e.getMessage(), e);
                    }
                }
                @Override
                public void connectionLost(Throwable cause) {
                    Toast.makeText(getApplicationContext(), cause.getMessage() + "连接失败！！！", Toast.LENGTH_LONG).show();
                }
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    try {
                        String payload = new String(message.getPayload());
                        arrived(topic, payload);
                    } catch (Exception e) {
                        Log.e("MainActivity", e.getMessage(), e);
                        publish(TOPIC + "error", e.getMessage().getBytes(StandardCharsets.UTF_8));
                    }
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            options.setUserName(USERNAME);
            options.setPassword(PASSWORD.toCharArray());
            options.setWill("/touhuwai/offline", deviceId.getBytes(StandardCharsets.UTF_8), qos, true);
            mqttClient.connect(options, null, null).waitForCompletion();
            mqttConnectHandler.postDelayed(mqttConnectRunnable, 10000); // 10秒监测一次是否断连
            return true;
        } catch (MqttException e) {
            Log.e("MainActivity", e.getMessage(), e);
            return false;
        }
    }

    private void arrived (String topic, String payload) throws Exception {
        List<HiAdvItem> dataList = new ArrayList<>();
        if (payload == null || "".equals(payload)) {
//                            dataList.add(new Advance(R.drawable.img, 0));
        } else {
            long currentTimeMillis = System.currentTimeMillis();
            lastMessageTime = currentTimeMillis;
            JSONObject jsonObject = new JSONObject(payload);
            if (topic == null) {
                topic = jsonObject.getString("topic");
            }
            JSONArray playList = jsonObject.getJSONArray("playList");
            boolean type = jsonObject.isNull("type");
            if (topic.startsWith(TOPIC_DEFAULT) || (!type && "default".equals(jsonObject.getString("type")))) {
                // 垫播列表，收到消息后先下载至本地，后续由监听器切换
                List<HiAdvItem> hiAdvItems = messageToAdvance(playList, true, currentTimeMillis);
                if (isPlayDefault) {
                    MainActivity.this.runOnUiThread(() -> hi_adv_box.restartWork(hiAdvItems));
                }
                return;
            } else {
                dataList = messageToAdvance(playList, false, currentTimeMillis);
                if (!jsonObject.isNull("totalDuration")) {
                    int totalDuration = jsonObject.getInt("totalDuration");
                    mTimeRemaining = totalDuration;
                    mTimerHandler.removeCallbacks(mTimerRunnable);
                    mTimerHandler.postDelayed(mTimerRunnable, DELAY_TIME);
                }
            }
            List<HiAdvItem> finalDataList = dataList;
            if (currentTimeMillis == lastMessageTime) {
                progressBar.setVisibility(View.INVISIBLE);
                hi_adv_box.progress = progress;
                isPlayDefault = false;
                MainActivity.this.runOnUiThread(() -> hi_adv_box.restartWork(finalDataList));
            }
        }
    }

    public void publish(String topic, byte[] payload) {
        try {
            MqttMessage message = new MqttMessage(payload);
            mqttClient.publish(topic, message).waitForCompletion();
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    private ServerDto getServerInfo() {
        Cursor mqttServer = db.rawQuery(SELECT_MQTT_TABLE_SQL, null);
        if (mqttServer.getCount() == 0) {
            return null;
        }

        mqttServer.moveToFirst();
        ServerDto serverDto = new ServerDto();
        //  "tcp://192.168.5.120:1883"
        serverDto.url = mqttServer.getString(1).trim();
        serverDto.userName = mqttServer.getString(2).trim();
        serverDto.password = mqttServer.getString(3).trim();
        return serverDto;
    }

    private boolean checkMqttServer () {
//        if (!networkIsConnect()) { // 未联网，继续播放重启之前的内容
//            List<FileDto> fileDtoList = DbHelper.queryFileDtoListBySql(db, SELECT_OCCUPIED_FILE_SQL);
//            if (fileDtoList.size() > 0) {
//                List<HiAdvItem> dataList = new ArrayList<>();
//                for (FileDto fileDto : fileDtoList) {
//                    dataList.add(new HiAdvItem(TYPE_MAP.get(fileDto.), fileDto., fileDto.url, fileDto.path));
//                }
//            }
//        }
        ServerDto serverInfo = getServerInfo();
        if (serverInfo == null) {
            return false;
        }
        boolean mqttConnect = mqtt(serverInfo.url, serverInfo.userName, serverInfo.password);
        if (mqttConnect) {
            return true;
        } else {
//            String sql = "delete from " + MQTT_TABLE;
//            db.execSQL(sql);
            return false;
        }
    }

    private List<HiAdvItem> messageToAdvance (JSONArray playList, boolean isDefaultPlay, long currentTimeMillis) throws Exception {
        List<HiAdvItem> dataList = new ArrayList<>();
        List<Object> currentPlayList = new ArrayList<>();
        List<HiAdvItem> defaultList = new ArrayList<>();
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
                HiAdvItem hiAdvItem = downDefaultPlay(fileUrl, type, duration, i == 0);
                defaultList.add(hiAdvItem);
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
        if (isDefaultPlay) {
         return defaultList;
        }
        return dataList;
    }

    private HiAdvItem downDefaultPlay (String fileUrl, String type, Integer duration, boolean isDelete) {
        Cursor cursor = db.rawQuery(SELECT_DEFAULT_TABLE_SQL,null);
        int count = cursor.getCount();
        if (count != 0 && isDelete) { // 删除现有垫播信息
            cursor.moveToFirst();  //移动到首位
            for (int i = 0; i < cursor.getCount(); i++) {
                String path = cursor.getString(2);
                int occupy = cursor.getInt(6);
                int id = cursor.getInt(0);
                if (occupy != FILE_OCCUPY) {
                    if (path != null) {
                        FileUtils.deleteTempFile(new File(path), DEFAULT_RETRY_COUNT);
                    }
                } else {
                    db.execSQL(UPDATE_DEFAULT_TABLE_NOCCUPIED_SQL + " and id = " + id); // 更新为未占用
                }
                //移动到下一位
                cursor.moveToNext();
            }
            db.execSQL(DELETE_DEFAULT_TABLE_SQL);
        }
        ContentValues cValue = new ContentValues();
        cValue.put("url", fileUrl);
        String path = null;
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            String filePath = defaultFileDir + fileName;
            if (!new File(filePath).exists()) {
                path = FileUtils.downFile(fileUrl, defaultFileDir, db);
            } else {
                path = filePath;
            }
            cValue.put("path", path);
            cValue.put("type", type);
            cValue.put("duration", duration);
            cValue.put("occupy", FILE_OCCUPY);
            cValue.put("status", FILE_DOWN_STATUS_SUCCESS);
        } catch (Exception e) {
            cValue.put("status", FILE_DOWN_STATUS_ERROR);
            Log.e("MainActivity", e.getMessage(), e);
        } finally {
            String sql = SELECT_DEFAULT_TABLE_SQL + " and url = '" + fileUrl + "'";
            Cursor queryFromDb = db.rawQuery(sql,null);
            if (queryFromDb.getCount() > 0) {
                queryFromDb.moveToFirst();
                db.update(DEFAULT_TABLE, cValue, "id=?", new String[]{String.valueOf(queryFromDb.getInt(0))});
            } else {
                db.insert(DEFAULT_TABLE, null, cValue);
            }
            Log.e("FileUtils", fileUrl + "文件下载结束");
            return new HiAdvItem(TYPE_MAP.get(type), duration, fileUrl, String.valueOf(path));
        }

    }


    private Map<String, Object> queryFilePath (String imageUrl, boolean isForceDown, int retryCount) {
        Integer img = null;
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
        Object filePath = null;
        if (status == FILE_DOWN_STATUS_SUCCESS) {
            filePath = dbFiles.getString(2);
        } else {
            // 重试
            if (retryCount > 0) {
                retryCount--;
                int finalRetryCount = retryCount;
                Executors.newSingleThreadExecutor().execute(() -> queryFilePath(url, true, finalRetryCount));
            }
            filePath = null;
        }
        Integer id = dbFiles.getInt(0);
        dbFiles.close();
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("filePath", filePath);
        result.put("status", status);
        return result;
    }

    private void wifiRssi() {
        wifiTextView = findViewById(R.id.wifi_text_view);
        int rssi = DeviceInfoUtil.getRssi(getApplicationContext());
        String text = "rssi:" + rssi;
        wifiTextView.setText(text);
        mqttConnectHandler.postDelayed(wifiRssiRunnable, 5); // 10秒监测一次是否断连
    }
    private Runnable wifiRssiRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                wifiTextView = findViewById(R.id.wifi_text_view);
                int rssi = DeviceInfoUtil.getRssi(getApplicationContext());
                String text = "rssi:" + rssi;
                wifiTextView.setText(text);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                mqttConnectHandler.postDelayed(this, 5); // 10秒监测一次是否断连
            }
        }
    };

    private void serverInit () {
        mServer = AndServer.webServer(this).port(18884)
                .timeout(10, TimeUnit.SECONDS).listener(new Server.ServerListener() {
                    @Override
                    public void onStarted() {
                    }
                    @Override
                    public void onStopped() {
                    }
                    @Override
                    public void onException(Exception e) {
                    }
                }).build();
        mServer.startup();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
//            case KeyEvent.KEYCODE_ENTER:     //确定键enter
//            case KeyEvent.KEYCODE_DPAD_CENTER:
//                Log.d(TAG,"enter--->");
//                break;
//            case KeyEvent.KEYCODE_BACK:    //返回键
//                Log.d(TAG,"back--->");
//                setContentView(R.layout.activity_login);
//                return true;   //这里由于break会退出，所以我们自己要处理掉 不返回上一层
            case KeyEvent.KEYCODE_0:   //数字键0
                Log.d(TAG,"0--->");
                destroy();
                showView(true);
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
        destroy();
    }

    private void destroy() {
        mServer.shutdown();
        // 断开MQTT连接
        try {
            mqttClient.disconnect();
            mqttConnectHandler.removeCallbacks(mqttConnectRunnable);
            mqttConnectHandler.removeCallbacks(wifiRssiRunnable);
            mqttConnectHandler.removeCallbacks(mqttReConnectRunnable);
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void msg (Event event) throws Exception {
        System.out.println(event.getMsg());
        arrived(null, event.getMsg());
    }

    public boolean networkIsConnect () {
        // 获取 ConnectivityManager 对象
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // 获取当前默认网络的信息，可能为空(null)
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        // 判断网络状态是否可用
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private Handler mqttConnectHandler = new Handler();
    private Runnable mqttConnectRunnable = new Runnable() {
        @Override
        public void run() {
            Boolean connected = null;
            try {
                connected = mqttClient.isConnected();
                if (!connected) {
                    mqttClient.connect(options, null, null).waitForCompletion();
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                Log.d(TAG, "监测是否断连， 当前isConnected：" + connected);
                mqttConnectHandler.postDelayed(this, 10000); // 10秒监测一次是否断连
            }
        }
    };

    private Runnable mqttReConnectRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                String serverIp = mServerIpEditText.getText().toString().trim();
                String username = mUsernameEditText.getText().toString().trim();
                String password = mPasswordEditText.getText().toString().trim();
                if (serverIp.contains(":")) {
                    serverIp = "tcp://" + serverIp;
                } else {
                    serverIp = "tcp://" + serverIp + ":1883";
                }
                boolean mqttConnect = mqtt(serverIp, username, password);
                if (mqttConnect) {
                    connectSuccess();
                } else {
                    mqttConnectHandler.postDelayed(this, 5); // 5s重连一次
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    };
}