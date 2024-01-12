package com.touhuwai.control;

import static com.touhuwai.control.db.DbHelper.FILE_DOWN_STATUS_SUCCESS;
import static com.touhuwai.control.db.DbHelper.FILE_OCCUPY;
import static com.touhuwai.control.db.DbHelper.MQTT_TABLE;
import static com.touhuwai.control.db.DbHelper.SELECT_DEFAULT_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.SELECT_MQTT_TABLE_SQL;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.liulishuo.okdownload.DownloadTask;
import com.touhuwai.control.db.DbHelper;
import com.touhuwai.control.entry.Event;
import com.touhuwai.control.entry.FileDto;
import com.touhuwai.control.entry.ServerDto;
import com.touhuwai.control.entry.Topic;
import com.touhuwai.control.utils.BroadcastUtils;
import com.touhuwai.control.utils.DeviceInfoUtil;
import com.touhuwai.control.utils.FileDownUtils;
import com.touhuwai.control.utils.FileUtils;
import com.touhuwai.control.utils.JsonUtils;
import com.touhuwai.control.utils.LogToFile;
import com.touhuwai.control.utils.RandomNumberUtil;
import com.touhuwai.control.utils.ThreadUtils;
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
    private final MqttClientPersistence persistence = new MemoryPersistence();
    private String fileDir, deviceId, defaultFileDir;
    private EditText mServerIpEditText, mUsernameEditText, mPasswordEditText;
    private static final int REQUEST_READ_PHONE_STATE = 1;

    private static final int DELAY_TIME = 1000; // 每秒检测是否需要播垫播素材
    private int defaultPlayDifferenceTime;
    private ProgressBar progressBar;
    private boolean isPlayDefault = true;
    private final int qos = 0;

    private TextView wifiTextView;

    private Server mServer;
    private final FileDownUtils fileDownUtils = new FileDownUtils();

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
        fileDir = FileUtils.getFilePath(this.getApplicationContext(), "files/");
        defaultFileDir = FileUtils.getFilePath(this.getApplicationContext(), "default/");
        deviceId = DeviceInfoUtil.getDeviceId(this.getApplicationContext());
        LogToFile.createLogFile(this.getApplicationContext());
        LogToFile.writeLogToFile();
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
        mqttConnectHandler.postDelayed(checkSdFreeRunnable, 20000);
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
            Button mConnectButton = findViewById(R.id.connect);
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
                    DbHelper.deleteServerInfo(db);
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
        mqttConnectHandler.removeCallbacks(mqttReConnectRunnable);
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
                        for (String topic : Topic.TOPIC_ARRAY) {
                            mqttClient.subscribe(topic + randomString + deviceId, qos).waitForCompletion();
                        }
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
                        publish(Topic.PLAYER + "error", e.getMessage().getBytes(StandardCharsets.UTF_8));
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
        if (!(payload == null || "".equals(payload))) {
            Log.d(TAG, payload);
            long currentTimeMillis = System.currentTimeMillis();
            lastMessageTime = currentTimeMillis;
            JSONObject jsonObject = new JSONObject(payload);
            if (topic == null) {
                topic = jsonObject.getString("topic");
            }
             if (topic.startsWith(Topic.SHUTDOWN)) {
                 FileUtils.deleteDirectoryFiles(db, fileDir);
                BroadcastUtils.shutdown(this.getApplicationContext());
            } else if (topic.startsWith(Topic.POWER_ON_ALARM)) {
                 FileUtils.deleteDirectoryFiles(db, fileDir);
                 String startAtTime = jsonObject.getString("startTime");
                 BroadcastUtils.setPowerOnAlarm(Long.parseLong(startAtTime));
                 BroadcastUtils.shutdown(this.getApplicationContext());
            } else {
                JSONArray playList = jsonObject.getJSONArray("playList");
                boolean type = jsonObject.isNull("type");
                if (topic.startsWith(Topic.PLAYER_DEFAULT) || (!type && "default".equals(jsonObject.getString("type")))) {
                    handlerDefaultPlayMessage(playList, currentTimeMillis);
                } else {
                    handlerMessage(playList, currentTimeMillis, jsonObject);
                }
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

    private void handlerDefaultPlayMessage (JSONArray playList, long currentTimeMillis) throws Exception {
        Map<String, JSONObject> playListMap = new HashMap<>();
        Map<String, FileDto> fileCache = new HashMap<>();
        List<Object> playIds = new ArrayList<>();
        JSONArray unDownPlayList = new JSONArray();
        Map<String, HiAdvItem> hiAdvItemMap = new HashMap<>();

        // 1. 遍历消息内容，获取已下载消息文件
        for (int i = 0; i < playList.length(); i++) {
            JSONObject item = playList.getJSONObject(i);
            String fileUrl = item.getString("url");
            FileDto fileDto = DbHelper.queryDefaultByUrl(db, fileUrl);
            if (fileDto != null) {
                fileCache.put(fileUrl, fileDto);
                playIds.add(fileDto.id);
                hiAdvItemMap.put(fileUrl, HiAdvItem.build(item, fileDto.path));
            } else {
                unDownPlayList.put(item);
            }
            playListMap.put(fileUrl, item);
        }

        fileDownUtils.stopDefaultDownloads(unDownPlayList);

        // 2. 将未下载文件消息放入下载队列
        List<DownloadTask> endTask = new ArrayList<>();
        fileDownUtils.downloadFiles(true, unDownPlayList, defaultFileDir, (task, success) -> {
            // 任务结束监听中构建播放所需内容， 记录已结束下载任务个数
            endTask.add(task);
            String fileUrl = task.getUrl();
            if (success) {
                String filePath = task.getFile().getPath();
                JSONObject item = playListMap.get(fileUrl);
                String type = JsonUtils.getString(item, "type");
                HiAdvItem hiAdvItem = HiAdvItem.build(item, filePath);
                long id = DbHelper.insertDefaultFile(db, fileUrl, filePath, type, hiAdvItem.getResourceDuration(), FILE_OCCUPY, FILE_DOWN_STATUS_SUCCESS);
                playIds.add(id);
                hiAdvItemMap.put(fileUrl, hiAdvItem);
            }
            Log.d(TAG, "下载垫播文件结束：" + fileUrl + "  endTask.size() " + endTask.size() + "   fileCache.size() " + fileCache.size() + "   playList.length() " + playList.length());
        });

        Executors.newSingleThreadExecutor().execute(() -> {
            // 3. 异步轮询消息内容是否下载完毕
            while (true) {
                if (currentTimeMillis < lastMessageTime) {
                    break;
                }
                int currentProgress = endTask.size() + fileCache.size();
                setProgress(currentProgress, playList.length());
                if (currentProgress == playList.length()) {
                    List<HiAdvItem> dataList = new ArrayList<>();
                    for (int i = 0; i < playList.length(); i++) {
                        JSONObject item = JsonUtils.getJSONObject(playList, i);
                        if (item != null) {
                            String fileUrl = JsonUtils.getString(item, "url");
                            HiAdvItem hiAdvItem = hiAdvItemMap.get(fileUrl);
                            if (hiAdvItem != null) {
                                dataList.add(hiAdvItem);
                            }
                        }
                    }
                    DbHelper.updateDefaultOccupyFile(db, playIds);
                    if (isPlayDefault) {
                        MainActivity.this.runOnUiThread(() -> hi_adv_box.restartWork(dataList));
                    }
                    break;
                } else {
                    ThreadUtils.sleep(1000);
                }
            }
        });
    }

    private void handlerMessage (JSONArray playList, long currentTimeMillis, JSONObject message) throws Exception {
        Map<String, JSONObject> playListMap = new HashMap<>();
        Map<String, FileDto> fileCache = new HashMap<>();
        List<Object> currentPlayIds = new ArrayList<>();
        JSONArray unDownPlayList = new JSONArray();
        Map<String, HiAdvItem> hiAdvItemMap = new HashMap<>();

        // 1. 遍历消息内容，获取已下载消息文件
        for (int i = 0; i < playList.length(); i++) {
            JSONObject item = playList.getJSONObject(i);
            String fileUrl = item.getString("url");
            FileDto fileDto = DbHelper.queryByUrl(db, fileUrl);
            if (fileDto != null) {
                fileCache.put(fileUrl, fileDto);
                currentPlayIds.add(fileDto.id);
                hiAdvItemMap.put(fileUrl, HiAdvItem.build(item, fileDto.path));
            } else {
                unDownPlayList.put(item);
            }
            playListMap.put(fileUrl, item);
        }

        fileDownUtils.stopDownloads(unDownPlayList); // 删除前一次正在进行的任务
        // 2. 将未下载文件消息放入下载队列
        List<DownloadTask> endTask = new ArrayList<>();
        fileDownUtils.downloadFiles(false, unDownPlayList, fileDir, (task, success) -> {
            // 任务结束监听中构建播放所需内容， 记录已结束下载任务个数
            endTask.add(task);
            String fileUrl = task.getUrl();
            if (success) {
                String filePath = task.getFile().getPath();
                JSONObject item = playListMap.get(fileUrl);
                long id = DbHelper.insertFile(db, fileUrl, FILE_OCCUPY, filePath, FILE_DOWN_STATUS_SUCCESS);
                currentPlayIds.add(id);
                hiAdvItemMap.put(fileUrl, HiAdvItem.build(item, filePath));
            } else {
                DbHelper.insertFailFile(db, fileUrl, task.getFile().getPath());
            }
            Log.d(TAG, "下载播放列表文件结束：" + fileUrl +"  endTask.size() " + endTask.size() + "   fileCache.size() " + fileCache.size() + "   playList.length() " + playList.length());
        });

        Executors.newSingleThreadExecutor().execute(() -> {
            // 3. 异步轮询消息内容是否下载完毕
            while (true) {
                if (currentTimeMillis < lastMessageTime) { break; }

                int currentProgress = endTask.size() + fileCache.size();
                setProgress(currentProgress, playList.length());
                if (currentProgress == playList.length()) {
                    if (!message.isNull("totalDuration")) {
                        defaultPlayDifferenceTime = JsonUtils.getInt(message, "totalDuration");
                        defaultPlayHandler.removeCallbacks(defaultPlayRunnable);
                        defaultPlayHandler.postDelayed(defaultPlayRunnable, DELAY_TIME);
                    }
//                    progressBar.setVisibility(View.INVISIBLE);

                    isPlayDefault = false;
                    List<HiAdvItem> dataList = new ArrayList<>();
                    for (int i = 0; i < playList.length(); i++) {
                        JSONObject item = JsonUtils.getJSONObject(playList, i);
                        if (item != null) {
                            String fileUrl = JsonUtils.getString(item, "url");
                            HiAdvItem hiAdvItem = hiAdvItemMap.get(fileUrl);
                            if (hiAdvItem != null) {
                                dataList.add(hiAdvItem);
                            }
                        }
                    }
                    MainActivity.this.runOnUiThread(() -> hi_adv_box.restartWork(dataList));
                    DbHelper.updateOccupyFile(db, currentPlayIds);
                    break;
                } else {
                    ThreadUtils.sleep(1000);
                }
            }
        });
    }

    private void setProgress (int index, int sum) {
        double v = index * 1.0 / sum * 100;
        progress = (int) v;
        progressBar.setProgress(progress);
        hi_adv_box.progress = progress;
    }


    private void wifiRssi() {
        wifiTextView = findViewById(R.id.wifi_text_view);
        String text = "rssi:" + DeviceInfoUtil.getRssi(getApplicationContext());
        wifiTextView.setText(text);
        mqttConnectHandler.postDelayed(wifiRssiRunnable, 5); // 10秒监测一次是否断连
    }
    private Runnable wifiRssiRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                wifiTextView = findViewById(R.id.wifi_text_view);
                String text = "rssi:" + DeviceInfoUtil.getRssi(getApplicationContext());
                wifiTextView.setText(text);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                mqttConnectHandler.postDelayed(this, 2000); // 10秒监测一次是否断连
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
            mqttConnectHandler.removeCallbacks(checkSdFreeRunnable);
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

    private final Handler defaultPlayHandler = new Handler();
    private final Runnable defaultPlayRunnable = new Runnable() {
        @Override
        public void run() {
            if (defaultPlayDifferenceTime == 0) {
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
                if (dataList.size() > 0) {
                    MainActivity.this.runOnUiThread(() -> hi_adv_box.restartWork(dataList));
                } else {
                    defaultPlayHandler.postDelayed(this, DELAY_TIME);
                }
            } else {
                defaultPlayDifferenceTime --;
                defaultPlayHandler.postDelayed(this, DELAY_TIME);
            }
        }
    };
    private final Handler mqttConnectHandler = new Handler();
    private final Runnable mqttConnectRunnable = new Runnable() {
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
    private final Runnable mqttReConnectRunnable = new Runnable() {
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

    private final Runnable checkSdFreeRunnable = new Runnable()  {
        @Override
        public void run() {
            FileUtils.checkSdFree(db);
            mqttConnectHandler.postDelayed(this, 60000 * 1);
        }
    };

}