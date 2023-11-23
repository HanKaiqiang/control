package com.touhuwai.control.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;
import com.liulishuo.okdownload.core.listener.DownloadListener2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FileDownloader {

    String TAG = "FileDownloader";
    private List<DownloadTask> runningTasks = new ArrayList<>();
    private Map<String, DownloadTask> runningTaskMap = new HashMap<>();

    private List<DownloadTask> defaultRunningTasks = new ArrayList<>(); // 垫播下载队列
    private Map<String, DownloadTask> defaultRunningTaskMap = new HashMap<>();


    public FileDownloader() {
        DownloadDispatcher.setMaxParallelRunningCount(5);

    }

    public void downloadFiles(boolean isDefault, JSONArray playList, String fileDir, final DownloadCallback callback) throws Exception {
        if (playList == null || playList.length() == 0) {
            return;
        }

        List<DownloadTask> tasks = new ArrayList<>();
        // 遍历消息，获取文件下载地址，批量下载
        for (int i = 0; i < playList.length(); i++) {
            JSONObject item = playList.getJSONObject(i);
            String fileUrl = item.getString("url");
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            String[] split = fileName.split("\\.");
            fileName = UUID.randomUUID().toString().substring(0, 8) + System.currentTimeMillis() + "." + split[1];
            DownloadTask task = new DownloadTask.Builder(fileUrl, fileDir, fileName).setConnectionCount(1).build();
            tasks.add(task);
            if (isDefault) {
                defaultRunningTasks.add(task);
                defaultRunningTaskMap.put(fileUrl, task);
            } else {
                runningTasks.add(task);
                runningTaskMap.put(fileUrl, task);
            }
        }

        DownloadListener listener = new DownloadListener2() {
            @Override
            public void taskStart(@NonNull DownloadTask task) {
                Log.d(TAG, "task开始：" + task.getUrl());
            }

            @Override
            public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause) {
                String errorMsg = realCause == null ? "" : (realCause.getMessage() == null ? "" : realCause.getMessage());
                if (cause == EndCause.COMPLETED) {
                    callback.onFileDownloaded(task, true);
                } else if (cause == EndCause.ERROR && errorMsg.contains("The current offset on block-info isn't update correct")) {
                    callback.onFileDownloaded(task, true);
                } else {
                    callback.onFileDownloaded(task, false);
                }
            }
        };
        DownloadTask.enqueue(tasks.toArray(new DownloadTask[0]), listener);
    }


    public void stopDownloads(JSONArray playList) {
        List<DownloadTask> cancelTasks = new ArrayList<>();
        cancelTasks.addAll(runningTasks);
        for (int i = 0; i < playList.length(); i++) {
            JSONObject jsonObject = JsonUtils.getJSONObject(playList, i);
            if (jsonObject != null) {
                String url = JsonUtils.getString(jsonObject, "url");
                DownloadTask downloadTask = runningTaskMap.get(url);
                if (downloadTask != null) {
                    cancelTasks.remove(downloadTask);
                }
            }
        }

        DownloadTask[] taskArray = {};
        DownloadTask.cancel(cancelTasks.toArray(taskArray));
        runningTasks.removeAll(cancelTasks);
    }

    public void stopDefaultDownloads(JSONArray playList) {
        List<DownloadTask> cancelTasks = new ArrayList<>();
        cancelTasks.addAll(defaultRunningTasks);
        for (int i = 0; i < playList.length(); i++) {
            JSONObject jsonObject = JsonUtils.getJSONObject(playList, i);
            if (jsonObject != null) {
                String url = JsonUtils.getString(jsonObject, "url");
                DownloadTask downloadTask = defaultRunningTaskMap.get(url);
                if (downloadTask != null) {
                    cancelTasks.remove(downloadTask);
                }
            }
        }

        DownloadTask[] taskArray = {};
        DownloadTask.cancel(cancelTasks.toArray(taskArray));
        defaultRunningTasks.removeAll(cancelTasks);
    }

    public void deleteDownloads() {
        for (DownloadTask runningTask : runningTasks) {
            OkDownload.with().breakpointStore().remove(runningTask.getId());
        }
    }


    public interface DownloadCallback {
        void onFileDownloaded(DownloadTask task, boolean success);
    }


}
