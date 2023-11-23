package com.touhuwai.control.utils;

import android.net.TrafficStats;

import java.text.DecimalFormat;

public class NetworkSpeedUtil {

    private static long lastRxBytes = 0;
    private static long lastTxBytes = 0;
    private static long lastUpdateTime = 0;

    public static String getNetworkSpeed() {
        long currentRxBytes = TrafficStats.getTotalRxBytes();
        long currentTxBytes = TrafficStats.getTotalTxBytes();
        long currentTime = System.currentTimeMillis();

        // 计算时间差和流量差
        long timeInterval = currentTime - lastUpdateTime;
        long rxBytesInterval = currentRxBytes - lastRxBytes;
        long txBytesInterval = currentTxBytes - lastTxBytes;

        // 计算下载速度和上传速度
        float rxSpeed = rxBytesInterval * 1000 / timeInterval;
        float txSpeed = txBytesInterval * 1000 / timeInterval;

        // 更新上次的流量信息
        lastUpdateTime = currentTime;
        lastRxBytes = currentRxBytes;
        lastTxBytes = currentTxBytes;

        // 格式化速度值
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String rxSpeedString = decimalFormat.format(rxSpeed);
        String txSpeedString = decimalFormat.format(txSpeed);

        return rxSpeedString + " KB/s";
    }
}
