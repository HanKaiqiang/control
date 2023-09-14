package com.touhuwai.control.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastUtils {

    private static final String TAG = "BroadcastUtils";

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
    public static void powerOnAlarm (Context context, long times) {
//        CommandUtil.executeAlarmOnTime(0);
//        CommandUtil.executeAlarmOnTime(time);
        Intent intent = new Intent("android.intent.action.setpoweron");
        intent.putExtra("poweronutc", times);
        intent.putExtra("checkpowerontime",true);
        context.sendBroadcast(intent);
    }


    /**
     * 定时关机
     */
    public static void powerOffAlarm (Context context, long times) {
        Intent intent = new Intent("android.intent.action.setpoweroff");
        intent.putExtra("poweroffutc", times);
        intent.putExtra("checkpowerofftime",true);
        context.sendBroadcast(intent);
    }


    public static void setPowerOnAlarm(long time) {
        Log.d(TAG, "setPowerOnAlarm: ----time=" + time);
        CommandUtil.executeAlarmOnTime(0);
        CommandUtil.executeAlarmOnTime(time);
    }

}
