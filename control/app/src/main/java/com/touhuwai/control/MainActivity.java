package com.touhuwai.control;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.touhuwai.hiadvbox.HiAdvBox;
import com.touhuwai.hiadvbox.HiAdvItem;
import com.touhuwai.hiadvbox.IAdvPlayEventListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    HiAdvBox hi_adv_box;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fullScreen();



        setContentView(R.layout.activity_main);

        init();
        //startPlay();
    }
    private static final int REQUEST_READ_PHONE_STATE = 1;

    private void init(){
        AndPermission.with(this)
                .runtime()
                .permission(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
                .onGranted(permissions -> {
                    // Storage permission are allowed.
                    //grantOnTop();
                    startPlay();
                })
                .onDenied(permissions -> {
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_READ_PHONE_STATE);
                    // Storage permission are not allowed.
                })
                .start();
    }

    private void startPlay(){
        hi_adv_box = findViewById(R.id.hi_adv_box);
        hi_adv_box.init(this);

        List<HiAdvItem> list = new ArrayList<>();
        //请先把myres资源手动拷贝到/mnt/sdcard/advpub 目录下。真正运行时，应该是从网上下载并自动放于/mnt/sdcard/advpub目录的
        String path = this.getExternalFilesDir("").getAbsolutePath();

        list.add(new HiAdvItem(UUID.randomUUID().toString(), 0, 2,  path + "/123.jpg"));
        list.add(new HiAdvItem(UUID.randomUUID().toString(), 1, 0, path + "/小河1620x1080.mp4"));
        list.add(new HiAdvItem(UUID.randomUUID().toString(), 0, 3, path + "/we.mp4"));
        list.add(new HiAdvItem(UUID.randomUUID().toString(), 1, 0, path + "/HKQ-119 2023-05-31 12-10-35.mp4"));
        //hi_adv_box.setData(list);
        //hi_adv_box.startWork();
        hi_adv_box.restartWork(list);
        hi_adv_box.setAdvEventListener(new IAdvPlayEventListener() {
            @Override
            public void onPlayAdvItemResult(boolean isSucceed, String resourceId, int resourceType, int actualDuration, Date startTime, Date endTime) {
                Log.i(TAG, "外部外部 播放一条 item played. resourceType=" + resourceType
                        + ", actualDuration=" + actualDuration
                        + ", startTime=" + startTime
                        + ", endTime=" + endTime
                );
            }
        });
    }


    private void fullScreen() {
        // 隐藏状态栏、标题栏
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 横屏
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // 设置全屏模式
        getWindow().getDecorView() .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_main);
    }

}