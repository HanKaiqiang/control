package com.touhuwai.control.utils;

import static android.os.Environment.MEDIA_MOUNTED;
import static com.touhuwai.control.db.DbHelper.DELETE_FILE_TABLE_SQL;
import static com.touhuwai.control.db.DbHelper.FAIL_FILE_TABLE;
import static com.touhuwai.control.db.DbHelper.FILE_DOWN_STATUS_ERROR;
import static com.touhuwai.control.db.DbHelper.FILE_DOWN_STATUS_SUCCESS;
import static com.touhuwai.control.db.DbHelper.FILE_OCCUPY;
import static com.touhuwai.control.db.DbHelper.FILE_TABLE;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.touhuwai.control.db.DbHelper;
import com.touhuwai.control.entry.FileDto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
        String[] split = fileName.split("\\.");
        fileName = UUID.randomUUID().toString().substring(0,8) + System.currentTimeMillis() + "." + split[1];
        String filePath = fileDir + fileName;
        filePath = downFileWithPath(fileUrl, filePath, db);
        return filePath;
    }

    public static String downFileWithPath(String fileUrl, String filePath, SQLiteDatabase db) throws Exception {
        URL url = new URL(fileUrl);
        Log.d("FileUtils", fileUrl + "文件下载开始");
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        FileOutputStream out = new FileOutputStream(filePath);
        byte[] outputByte = new byte[3 * 1024 * 1024];
        int r = -1;
        while ((r = in.read(outputByte)) != -1) {
            out.write(outputByte, 0, r);
        }
        out.close();
        in.close();
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
            Log.d("FileUtils", fileUrl + "文件下载结束");
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
//                        Log.d("FileUtils", "删除文件【" + tempFile + "】成功！");
                    } else {
                        Log.d("FileUtils", "删除文件【" + tempFile + "】失败！");
                        if (retryCount > 0) {
                            Integer nextCount = retryCount - 1;
                            Executors.newSingleThreadExecutor().execute(() -> {
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
        List<FileDto> fileDtoList = DbHelper.queryUnoccupiedFileDtoList(db);
        if (fileDtoList.size() > 0) {
            List<Integer> deletedIds = new ArrayList<>();
            for (int i = 0; i < fileDtoList.size(); i++) {
                FileDto fileDto = fileDtoList.get(i);
                String path = fileDto.path;
                if (path == null || "".equals(path)) {
                    continue;
                }
                File file = new File(path);
                if (!file.exists()) {
                    continue;
                }
                deletedIds.add(fileDto.id);
                FileUtils.deleteTempFile(file, 1);
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

        List<FileDto> fileDtos = DbHelper.queryFailFiles(db);
        if (fileDtos.size() > 0) {
            List<Integer> deletedIds = new ArrayList<>();
            for (int i = 0; i < fileDtos.size(); i++) {
                FileDto fileDto = fileDtos.get(i);
                String path = fileDto.path;
                if (path == null || "".equals(path)) {
                    return;
                }
                File file = new File(path);
                if (!file.exists()) {
                    continue;
                }
                deletedIds.add(fileDto.id);
                FileUtils.deleteTempFile(file, 1);
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
                db.execSQL("delete from " + FAIL_FILE_TABLE + " where 1 = 1 " + where);
            }
        }
    }


    public static void checkSdFree (SQLiteDatabase db) {
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long MIN_FREE_BYTES =(long)(statFs.getTotalBytes() * (1.0/3.0));
        long availableSize = statFs.getAvailableBytes();
        Log.d("FileUtils", "availableSize============================" + bytesToHuman(availableSize));
        Log.d("FileUtils", "MIN_FREE_BYTES============================" + bytesToHuman(MIN_FREE_BYTES));
        if (availableSize < MIN_FREE_BYTES) {
            deleteFiles(db);
        }
    }

    public static String floatForm (double d) {
        return new DecimalFormat("#.##").format(d);
    }

    public static String bytesToHuman (long size) {
        long Kb = 1  * 1024;
        long Mb = Kb * 1024;
        long Gb = Mb * 1024;
        long Tb = Gb * 1024;
        long Pb = Tb * 1024;
        long Eb = Pb * 1024;

        if (size <  Kb)                 return floatForm(        size     ) + " byte";
        if (size >= Kb && size < Mb)    return floatForm((double)size / Kb) + " Kb";
        if (size >= Mb && size < Gb)    return floatForm((double)size / Mb) + " Mb";
        if (size >= Gb && size < Tb)    return floatForm((double)size / Gb) + " Gb";
        if (size >= Tb && size < Pb)    return floatForm((double)size / Tb) + " Tb";
        if (size >= Pb && size < Eb)    return floatForm((double)size / Pb) + " Pb";
        if (size >= Eb)                 return floatForm((double)size / Eb) + " Eb";

        return "???";
    }


    public static void deleteDirectoryFiles(SQLiteDatabase db, String directoryPath) {
        File folder = new File(directoryPath);
        File[] files = folder.listFiles();
        long hourBefore = System.currentTimeMillis() - (1000 * 60 * 30);
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            long lastModified = file.lastModified();
            String path = file.getPath();
            if (lastModified < hourBefore) {
                FileDto fileDto = DbHelper.queryByPath(db, path);
                if (fileDto != null && !Objects.equals(fileDto.occupy, FILE_OCCUPY)) {
                    db.execSQL(DELETE_FILE_TABLE_SQL + " and path = '" + path + "'");
                    db.execSQL("delete from " + FAIL_FILE_TABLE + " where 1 = 1 " + " and path = '" + path + "'");
                    FileUtils.deleteTempFile(file, 1);
                }
            }
        }
    }


    public static void handleSSLHandshake() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("TLS");
            // trustAllCerts信任所有的证书
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception ignored) {
        }
    }

}
