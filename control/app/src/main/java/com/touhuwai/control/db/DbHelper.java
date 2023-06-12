package com.touhuwai.control.db;


import static com.touhuwai.control.utils.FileUtils.TYPE_MAP;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.touhuwai.control.entry.FileDto;
import com.touhuwai.hiadvbox.HiAdvItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DbHelper extends SQLiteOpenHelper  {

    public static final String  FILE_TABLE = "files";
    public static final String  MQTT_TABLE = "mqtt";

    public static final String  DEFAULT_TABLE = "default_play";

    public static String SELECT_FILE_TABLE_SQL = "select * from " + FILE_TABLE + " where 1 = 1 ";

    public static String UPDATE_FILE_UNOCCUPIED_SQL = "update " + FILE_TABLE + " set occupy = 0" + " where 1 = 1 ";

    public static String UPDATE_FILE_OCCUPIED_SQL = "update " + FILE_TABLE + " set occupy = 1" + " where 1 = 1 ";

    public static String DELETE_FILE_TABLE_SQL = "delete from " + FILE_TABLE + " where 1 = 1 ";

    public static String SELECT_MQTT_TABLE_SQL = "select * from " + MQTT_TABLE + " where 1 = 1 ";

    public static String SELECT_DEFAULT_TABLE_SQL = "select * from " + DEFAULT_TABLE + " where 1 = 1 ";

    public static String DELETE_DEFAULT_TABLE_SQL = "delete from " + DEFAULT_TABLE + " where 1 = 1 and occupy = 0 ";

    public static String UPDATE_DEFAULT_TABLE_NOCCUPIED_SQL = "update " + DEFAULT_TABLE + " set occupy = 0" + " where 1 = 1 ";

    public static Integer FILE_DOWN_STATUS_SUCCESS = 1; // 成功
    public static Integer FILE_DOWN_STATUS_ERROR = 0; // 失败

    public static Integer FILE_OCCUPY = 1; // 占用
    public static Integer FILE_UNOCCUPIED = 0; // 未占用

    public static final String CREATE_FILES_TABLE = "create table IF NOT EXISTS files ("
            + "id integer primary key autoincrement, "
            + "url text, "
            + "path text, "
            + "status integer, " // 下载状态status 1成功  0失败
            + "occupy integer, " // 是否为当前播放列表内容 0 不是， 1是
            + "size text)";

    public static final String CREATE_DEFAULT_TABLE = "create table IF NOT EXISTS default_play ("
            + "id integer primary key autoincrement, "
            + "url text, "
            + "path text, "
            + "type text, "
            + "duration integer, "
            + "status integer, "
            + "occupy integer, "
            + "size text)";

    public static final String CREATE_MQTT_TABLE = "create table IF NOT EXISTS mqtt ("
            + "id integer primary key autoincrement, "
            + "server_ip text, "
            + "username text, "
            + "password text)";

    public DbHelper(Context context) {
        super(context, "touhuwai.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //执行SQL语句
        db.execSQL(CREATE_FILES_TABLE);
        db.execSQL(CREATE_MQTT_TABLE);
        db.execSQL(CREATE_DEFAULT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public static FileDto queryByUrl (SQLiteDatabase db, String url) {
        String sql = SELECT_FILE_TABLE_SQL + " and url = '" + url + "'";
        Cursor cursor = db.rawQuery(sql,null);
        FileDto fileDto = null;
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            fileDto = new FileDto();
            fileDto.id = cursor.getInt(0);
            fileDto.url = cursor.getString(1);
            fileDto.path = cursor.getString(2);
            fileDto.status = cursor.getInt(3);
            fileDto.occupy = cursor.getInt(4);
        }
        return fileDto;
    }

    public static List<FileDto> queryFileDtoList (SQLiteDatabase db) {
        Cursor cursor = db.rawQuery(SELECT_FILE_TABLE_SQL,null);
        List<FileDto> fileDtoList = new ArrayList<>();
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();  //移动到首位
            for (int i = 0; i < cursor.getCount(); i++) {
                FileDto fileDto = new FileDto();
                fileDto.id = cursor.getInt(0);
                fileDto.url = cursor.getString(1);
                fileDto.path = cursor.getString(2);
                fileDto.status = cursor.getInt(3);
                fileDto.occupy = cursor.getInt(4);
                fileDtoList.add(fileDto);
                //移动到下一位
                cursor.moveToNext();
            }
        }
        return fileDtoList;
    }

}


