package com.touhuwai.control.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogToFile {

    private static String TAG = "LogToFile";
    private static SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private static String logName = format.format(new Date()) + ".log";
    private static String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/logs/" + logName;

    public static void createLogFile(Context context) {
        try {
            String dirPath = FileUtils.getFilePath(context, "/logs");
            filePath = FileUtils.getFilePath(context, "/logs").toString() + logName;
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File logFile = new File(filePath);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeLogToFile() {
        try {
            // 设置输出日志的级别和标签，并将日志输出到文件中 todo 仅保留当天日志
            Runtime.getRuntime().exec("logcat -v time -f " + filePath + " *:*");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
