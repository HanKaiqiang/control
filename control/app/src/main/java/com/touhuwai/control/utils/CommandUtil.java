package com.touhuwai.control.utils;

import android.util.Log;

import java.io.DataOutputStream;

public class CommandUtil {
    private static final String TAG = "CommandUtil";

    public static void executeAlarmOnTime(long time) {
        Process process = null;
        DataOutputStream os = null;
        Log.d(TAG, "executeAlarmOnTime: time=" + time);
        try {
            String command = "echo " + time + " > " + "/sys/class/rtc/rtc0/wakealarm";
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            Log.d("runtime_exception:", e.getMessage());
        } finally {
            try {
                if (os != null)
                    os.close();
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}