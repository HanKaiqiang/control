package com.touhuwai.control.utils;

import static android.os.Environment.MEDIA_MOUNTED;


import static com.touhuwai.control.db.DbHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class FileUtils {

    private String SDPATH;

    public FileUtils() {
        //得到当前外部存储设备的目录
        // /SDCARD
        SDPATH = Environment.getExternalStorageDirectory() + "/";
    }

    public static String getFilePath(Context context, String lastDir) {
        String directoryPath = "";
        //判断SD卡是否可用
        if (MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            directoryPath = context.getExternalFilesDir(lastDir).getAbsolutePath() + File.separator;
        } else {
            //没内存卡就存手机机身内存中
            directoryPath = context.getFilesDir() + File.separator + lastDir;
        }

        File file = new File(directoryPath);
        //判断文件目录是否已经存在
        if (!file.exists()) {
            file.mkdirs();
        }
        return directoryPath;
    }


    public static String downFile(String fileUrl, String fileDir) throws Exception {
        // 获取文件名
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        String filePath = fileDir + fileName;
        URL url = new URL(fileUrl);
        Log.e("FileUtils", fileUrl + "文件下载开始");
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        FileOutputStream out = new FileOutputStream(filePath);
        byte[] outputByte = new byte[1024];
        int r = -1;
        while ((r = in.read(outputByte)) != -1) {
            out.write(outputByte, 0, r);
        }
        out.close();
        in.close();

        return filePath;
    }

    public static void downloadAndSaveFile(String fileUrl, String fileDir, SQLiteDatabase db) {
        ContentValues cValue = new ContentValues();
        cValue.put("url", fileUrl);
        try {
            String filePath = downFile(fileUrl, fileDir);
            cValue.put("path", filePath);
            cValue.put("status", FILE_DOWN_STATUS_SUCCESS);
//            cValue.put("size", totalSize);
        } catch (Exception e) {
            cValue.put("status", FILE_DOWN_STATUS_ERROR);
            Log.e("FileUtils", e.getMessage(), e);
        } finally {
            db.insert(FILE_TABLE, null, cValue);
            Log.e("FileUtils", fileUrl + "文件下载结束");
        }
    }

    public static void deleteTempFile(File tempFile, Integer retryCount) {
        if (tempFile != null) {
            //删除临时文件
            try {
                if (tempFile.exists() && tempFile.isFile()) {
                    if (tempFile.delete()) {
                        Log.d("FileUtils", "删除文件【" + tempFile + "】成功！");
                    } else {
                        Log.d("FileUtils", "删除文件【" + tempFile + "】失败！");
                        if (retryCount > 0) {
                            Integer nextCount = retryCount - 1;
                            CompletableFuture.runAsync(() -> {
                                try {
                                    Thread.sleep(5000L * retryCount);
                                } catch (InterruptedException e) {
                                    // do nothing
                                }
                                if (tempFile.delete()) {
                                    Log.d("FileUtils", retryCount + "重试删除文件【" + tempFile + "】成功！");
                                } else {
                                    Log.d("FileUtils", retryCount + "重试删除文件【" + tempFile + "】失败！");
                                    FileUtils.deleteTempFile(tempFile, nextCount);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("FileUtils", "删除文件失败：" + e.getMessage(), e);
            }
        }
    }

}
