package com.touhuwai.hiadvbox;

import static com.touhuwai.control.utils.FileUtils.DEFAULT_DURATION;
import static com.touhuwai.control.utils.FileUtils.TYPE_IMAGE;
import static com.touhuwai.control.utils.FileUtils.TYPE_MAP;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class HiAdvItem {
    //数据库专用
    //public String uniqueId;

    //资源唯一id
    private String resourceId;

    //0--picture,  1-video
    private int resourceType;

    //time second
    private int resourceDuration;

    private String resourceUrl;

    private String localResourceFilePath;   //本地文件路径及名称

    public HiAdvItem(String resourceId, int resourceType, int resourceDuration, String localResourceFilePath) {
        this.resourceId = resourceId;
        //this.uniqueId = UUID.randomUUID().toString();
        this.resourceType = resourceType;
        this.resourceDuration = resourceDuration;
        this.localResourceFilePath = localResourceFilePath;
        //this.resourceUrl = resourceUrl;
    }

    public HiAdvItem(Integer resourceType, Integer resourceDuration, String resourceUrl, String localResourceFilePath) {
        this.resourceId = UUID.randomUUID().toString();
        this.resourceType = resourceType == null ? 0 : resourceType;
        this.resourceDuration = resourceDuration == null ? 0 : resourceDuration;
        this.localResourceFilePath = localResourceFilePath;
        this.resourceUrl = resourceUrl;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public int getResourceType() {
        return resourceType;
    }

    public void setResourceType(int resourceType) {
        this.resourceType = resourceType;
    }

    public int getResourceDuration() {
        return resourceDuration;
    }

    public void setResourceDuration(int resourceDuration) {
        this.resourceDuration = resourceDuration;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public String getLocalResourceFilePath() {
        return localResourceFilePath;
    }

    public void setLocalResourceFilePath(String localResourceFilePath) {
        this.localResourceFilePath = localResourceFilePath;
    }

    @Override
    public String toString() {
        return "HiAdvItem{" +
                "resourceId='" + resourceId + '\'' +
                ", resourceType=" + resourceType +
                ", resourceDuration=" + resourceDuration +
                ", resourceUrl='" + resourceUrl + '\'' +
                ", localResourceFilePath='" + localResourceFilePath + '\'' +
                '}';
    }


    public static HiAdvItem build(JSONObject item, String filePath) {
        try {
            String fileUrl = item.getString("url");
            String type = item.getString("type");
            Integer duration = null;
            if (item.isNull("duration")) {
                if (TYPE_IMAGE.equals(type)) { // 图片播放时长为空时，设置默认时长5S
                    duration = DEFAULT_DURATION;
                }
            } else {
                duration = item.getInt("duration");
            }
            return new HiAdvItem(TYPE_MAP.get(type), duration, fileUrl, filePath);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
