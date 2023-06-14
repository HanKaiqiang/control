package com.touhuwai.hiadvbox;


public class Advance {

    public Object path; //路径  我使用的是本地绝对路径
    public String type; //类型 1、视频 2、图片 3、GIF

    public Integer duration; // 播放时长

    public static final String TYPE_VIDEO = "1";
    public static final String TYPE_IMAGE = "2";
    public static final String TYPE_GIF = "3";

    public final static int DEFAULT_DURATION  = 5000; // 默认播放5S

    public Advance(Object path, String type) {
        this.path = path;
        this.type = type;
    }

    public void setDuration (Integer duration) {
        this.duration = duration;
    }
}