package com.touhuwai.control.db;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DbHelper extends SQLiteOpenHelper  {

    public static final String  FILE_TABLE = "files";
    public static final String  MQTT_TABLE = "mqtt";

    public static final String  DEFAULT_TABLE = "default_play";

    public static String SELECT_FILE_TABLE_SQL = "select * from " + FILE_TABLE + " where 1 = 1 ";

    public static String DELETE_FILE_TABLE_SQL = "delete from " + FILE_TABLE + " where 1 = 1 ";

    public static String SELECT_MQTT_TABLE_SQL = "select * from " + MQTT_TABLE + " where 1 = 1 ";

    public static String SELECT_DEFAULT_TABLE_SQL = "select * from " + DEFAULT_TABLE + " where 1 = 1 ";

    public static String DELETE_DEFAULT_TABLE_SQL = "delete from " + DEFAULT_TABLE + " where 1 = 1 ";

    public static Integer FILE_DOWN_STATUS_SUCCESS = 0;
    public static Integer FILE_DOWN_STATUS_ERROR = 1;

    public static final String CREATE_FILES_TABLE = "create table IF NOT EXISTS files ("
            + "id integer primary key autoincrement, "
            + "url text, "
            + "path text, "
            + "status integer, "
            + "size text)";

    public static final String CREATE_DEFAULT_TABLE = "create table IF NOT EXISTS default_play ("
            + "id integer primary key autoincrement, "
            + "url text, "
            + "path text, "
            + "type text, "
            + "duration integer, "
            + "status integer, "
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

}


