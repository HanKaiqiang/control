package com.touhuwai.control.utils;

import android.content.Context;
import android.content.Intent;

public class BroadcastUtils {

    /**
     * 唤醒
     */
    public static void wakeup (Context context) {
        context.sendBroadcast(new Intent("com.yql.ad.wakeup"));
    }

    /**
     * 休眠
     */
    public static void sleep (Context context) {
        context.sendBroadcast(new Intent("com.yql.ad.sleep"));
    }

    /**
     * 重启
     */
    public static void reboot (Context context) {
        context.sendBroadcast(new Intent("com.yql.ad.reboot"));
    }

    /**
     * 关机
     */
    public static void shutdown (Context context) {
        context.sendBroadcast(new Intent("com.yql.ad.shutdown"));
    }

    /**
     * 定时开机
     */
    public static void powerOnAlarm () {

    }


    /**
     * 定时关机
     */
    public static void powerOffAlarm () {

    }
}
