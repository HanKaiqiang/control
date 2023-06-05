package com.touhuwai.control.utils;

import android.os.Handler;
import android.util.DisplayMetrics;
import android.widget.TextView;

public class MyRunnable implements Runnable {
    private DisplayMetrics dm;
    private TextView deviceInfoTextView;
    private Handler handler;

    public MyRunnable(DisplayMetrics dm, TextView deviceInfoTextView, Handler handler) {
        this.dm = dm;
        this.deviceInfoTextView = deviceInfoTextView;
        this.handler = handler;
    }

    public static final int DELAY = 50;

    float x = 0, y = 0, dx = 5, dy = 5;
    int screenWidth, screenHeight, width, height;
    @Override
    public void run() {
        if (screenWidth == 0) {
            // 获取屏幕尺寸
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;
        }
        if (width == 0 || height == 0) {
            // 获取 TextView 尺寸
            width = deviceInfoTextView.getWidth();
            height = deviceInfoTextView.getHeight();
        }

        // 计算下一次更新后的位置
        x += dx;
        y += dy;
        if (x < 0) {
            x = 0;
            dx = -dx;
        } else if (x + width > screenWidth) {
            x = screenWidth - width;
            dx = -dx;
        }
        if (y < 0) {
            y = 0;
            dy = -dy;
        } else if (y + height > screenHeight) {
            y = screenHeight - height;
            dy = -dy;
        }
        // 更新 TextView 的位置
        deviceInfoTextView.setX(x);
        deviceInfoTextView.setY(y);
        handler.postDelayed(this, DELAY);
    }
}
