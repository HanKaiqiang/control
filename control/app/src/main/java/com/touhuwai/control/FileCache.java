package com.touhuwai.control;

import android.database.sqlite.SQLiteDatabase;

import com.touhuwai.control.utils.FileUtils;

public class FileCache implements Runnable {

    SQLiteDatabase db;

    public FileCache(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public void run() {
        FileUtils.checkSdFree(db);
    }
}
