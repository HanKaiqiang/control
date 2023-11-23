package com.touhuwai.control.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtils {

    private final static String TAG = "JsonUtils";

    public static int getInt(JSONObject message, String name) {
        try {
            return message.getInt(name);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            return 0;
        }
    }

    public static String getString(JSONObject jsonObject, String name) {
        try {
            return jsonObject.getString(name);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            return "";
        }
    }

    public static JSONObject getJSONObject(JSONArray jsonArray, int index) {
        try {
            return jsonArray.getJSONObject(index);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

}
