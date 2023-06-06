package com.touhuwai.control.entry;


public class FileDto {
    public Integer id;
    public String url;
    public String path;
    public Integer status; // 下载状态status 1成功  0失败
    public Integer occupy; // 是否为当前播放列表内容 0 不是， 1是
}
