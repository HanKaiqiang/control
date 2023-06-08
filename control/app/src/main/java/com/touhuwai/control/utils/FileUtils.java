package com.touhuwai.control.utils;

import static android.os.Environment.MEDIA_MOUNTED;
import static com.touhuwai.control.db.DbHelper.DELETE_FILE_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.FILE_DOWN_STATUS_ERROR;
import static com.touhuwai.control.db.DbHelper.FILE_DOWN_STATUS_SUCCESS;
import static com.touhuwai.control.db.DbHelper.FILE_OCCUPY;
import static com.touhuwai.control.db.DbHelper.FILE_TABLE;
import static com.touhuwai.control.db.DbHelper.SELECT_FILE_TABLE_SQL;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.touhuwai.control.FileCache;
import com.touhuwai.control.db.DbHelper;
import com.touhuwai.control.entry.FileDto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FileUtils {

    public static final String TYPE_VIDEO = "1";
    public static final String TYPE_IMAGE = "2";
    public static final String TYPE_GIF = "3";

    public static final Map<String, Integer> TYPE_MAP = new HashMap<>();
    static {
        TYPE_MAP.put(TYPE_VIDEO, 1);
        TYPE_MAP.put(TYPE_IMAGE, 0);
        TYPE_MAP.put(TYPE_GIF, 0);
    }

    public final static int DEFAULT_DURATION  = 5; // 默认播放5S
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


    public static String downFile(String fileUrl, String fileDir, SQLiteDatabase db) throws Exception {
        // 获取文件名
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        String filePath = fileDir + fileName;
        URL url = new URL(fileUrl);
        Log.e("FileUtils", fileUrl + "文件下载开始");
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        FileOutputStream out = new FileOutputStream(filePath);
        byte[] outputByte = new byte[5* 1024 * 1024];
        int r = -1;
        while ((r = in.read(outputByte)) != -1) {
            out.write(outputByte, 0, r);
        }
        out.close();
        in.close();

        new Thread(new FileCache(db)).start();
        return filePath;
    }

    public static Map<String, Object> downloadAndSaveFile(String fileUrl, String fileDir, SQLiteDatabase db) {
        ContentValues cValue = new ContentValues();
        Map<String, Object> result = new HashMap<>();
        cValue.put("url", fileUrl);
        cValue.put("occupy", FILE_OCCUPY);
        String filePath = null;
        try {
            filePath = downFile(fileUrl, fileDir, db);
            result.put("filePath", filePath);
            cValue.put("path", filePath);
            cValue.put("status", FILE_DOWN_STATUS_SUCCESS);
//            cValue.put("size", totalSize);
        } catch (Exception e) {
            cValue.put("status", FILE_DOWN_STATUS_ERROR);
            Log.e("FileUtils", e.getMessage(), e);
        } finally {
            Log.e("FileUtils", fileUrl + "文件下载结束");
            FileDto fileDto = DbHelper.queryByUrl(db, fileUrl);
            long id = 0;
            if (fileDto != null) {
                id = fileDto.id;
                db.update(FILE_TABLE, cValue, "id=?", new String[]{String.valueOf(id)});
            } else {
                id = db.insert(FILE_TABLE, null, cValue);
            }
            result.put("id", id);
            return result;
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

    public static void deleteFiles(SQLiteDatabase db) {
        List<FileDto> fileDtoList = DbHelper.queryFileDtoList(db);
        if (fileDtoList.size() > 0) {
            List<Integer> deletedIds = new ArrayList<>();
            long bytesToDelete = Math.max(0, MIN_FREE_BYTES - bytesAvailable);
            for (int i = 0; i < fileDtoList.size(); i++) {
                FileDto fileDto = fileDtoList.get(i);
                int occupy = fileDto.occupy;
                if (FILE_OCCUPY == occupy) {
                    continue;
                }
                String path = fileDto.path;
                if (path == null || "".equals(path)) {
                    return;
                }
                File file = new File(path);
                if (!file.exists()) {
                    return;
                }
                bytesToDelete -= file.length();
                if (bytesToDelete < 0) {
                    break;
                }
                FileUtils.deleteTempFile(file, 3);
                deletedIds.add(fileDto.id);
            }
            if (!deletedIds.isEmpty()) {
                StringBuilder where = new StringBuilder(" and id in (");
                for (int i = 0; i < deletedIds.size(); i++) {
                    Integer deletedId = deletedIds.get(i);
                    where.append(deletedId);
                    if (i != deletedIds.size() -1) {
                        where.append(", ");
                    }
                }
                where.append(") ");
                db.execSQL(DELETE_FILE_TABLE_SQL + where);
            }
        }
    }

    private static StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
    private static long bytesAvailable = (long)statFs.getBlockSize() * (long)statFs.getAvailableBlocks();
    public static long MIN_FREE_BYTES =(long)(statFs.getTotalBytes() / (1.0/3.0));

    public static void checkSdFree (SQLiteDatabase db) {
        long availableSize = statFs.getAvailableBytes();
        if (availableSize < MIN_FREE_BYTES) {
            deleteFiles(db);
        }
    }

}
