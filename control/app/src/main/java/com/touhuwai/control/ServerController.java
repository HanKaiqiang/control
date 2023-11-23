package com.touhuwai.control;

import android.util.Log;

import com.touhuwai.control.entry.Event;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestBody;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.http.HttpRequest;

import org.greenrobot.eventbus.EventBus;


@RestController
public class ServerController {
    @GetMapping("/")
    public String ping() {
        return "SERVER OK";
    }

    @PostMapping("/publish")
    public String publish(HttpRequest request, @RequestBody String jsonStr) {
        String appKey = request.getHeader("appKey");
        if ("touhuwai".equals(appKey)) {
            Log.d("ServerController", jsonStr);
            Event event = new Event(200, jsonStr);
            EventBus.getDefault().post(event);
            return "请求成功！";
        } else {
            return "请求失败！";
        }
    }

}
