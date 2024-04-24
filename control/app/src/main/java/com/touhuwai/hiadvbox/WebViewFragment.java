package com.touhuwai.hiadvbox;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.touhuwai.control.R;
import com.yanzhenjie.andserver.util.StringUtils;

import java.util.Date;


public class WebViewFragment extends MyFragment {
    private static final String TAG = WebViewFragment.class.getSimpleName();
    WebView webView;
    IAdvPlayEventListener mListener;
    HiAdvItem mAdvItem;


    public static synchronized Fragment newInstance(HiAdvItem advItem, IAdvPlayEventListener listener) {
        return new WebViewFragment(advItem, listener);
    }

    public WebViewFragment(HiAdvItem advItem, IAdvPlayEventListener listener) {
        mAdvItem = advItem;
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView, imageUrl=" + mAdvItem.getResourceUrl());
        View view = inflater.inflate(R.layout.fragment_webview, container, false);

        webView = view.findViewById(R.id.wv_webview);
        //Android webView加载Html5页面，JS不能调用问题和click事件无效的解决方法
        WebSettings s = webView.getSettings();
        s.setBuiltInZoomControls(true);      //进行控制缩放
        //  让webview只显示一列，也就是自适应页面大小 不能左右滑动，但在使用中发现，只针对4.4以下有效
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        s.setUseWideViewPort(true);     //让Webivew支持标签的viewport属性,前端html页面适配屏幕一般都会用到这个设置
        s.setLoadWithOverviewMode(true);
        s.setSavePassword(true);    //是否保存密码
        s.setSaveFormData(true);     //设置WebView是否保存表单数据，默认true，保存数据
        s.setJavaScriptEnabled(true);     // enable navigator.geolocation    允许支持js
        s.setJavaScriptCanOpenWindowsAutomatically(true);//设置脚本是否允许自动打开弹窗，默认false，不允许
        s.setGeolocationEnabled(true);     //定位是否可用，默认为true
        //可以在数据库中存储历史位置和Web初始权限
        s.setGeolocationDatabasePath("/data/data/org.itri.html5webview/databases/");     // enable Web Storage: localStorage, sessionStorage
        s.setDomStorageEnabled(true);  //启动webview的html5的本地存储功能,DOM存储API是否可用，默认false。
        webView.requestFocus();  //把输入焦点放在调用这个方法的控件上
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        //重写alert()，避免js的alert()无效
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                result.confirm();
                return true;
            }
        });

//        在Android4.0以后，会发现，只要是写在主线程（就是Activity）中的HTTP请求，运行时都会报错，这是因为Android在4.0以后为了防止
//        应用的ANR（Aplication Not Response）异常，Android这个设计是为了防止网络请求时间过长而导致界面假死的情况发生。
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        webView.setVisibility(View.VISIBLE);
        //系统默认会通过手机浏览器打开网页；为了能够直接通过webview显示网页，则必须设置
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            该接口，主要是给webview提供时机，让其选择是否对URLloading进行拦截；
//            关于该接口的返回值，true--拦截webview加载url；false--允许webview加载url
                view.loadUrl(url);
                return false;
            }
        });

        return view;
    }

    private String url = "https://www.touhuwai.com/";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String resourceUrl = mAdvItem.getResourceUrl();

        webView.loadUrl(StringUtils.isEmpty(resourceUrl) ? url : resourceUrl);
    }

    @Override
    public void onResume() {
        super.onResume();
        String localResourceFilePath = mAdvItem.getLocalResourceFilePath();
        if (localResourceFilePath != null && !localResourceFilePath.equals("null") && !localResourceFilePath.isEmpty()) {
            new Thread(new MyThread(mAdvItem.getResourceDuration(), this)).start();
        } else {
            new Thread(new MyThread(0, this)).start();
        }
    }

    public class MyThread implements Runnable {
        private int tDuration = 5;

        private WebViewFragment fragment;

        public MyThread(int duration, WebViewFragment fragment) {
            tDuration = duration;
            this.fragment = fragment;
        }

        int countSec = 0;

        @Override
        public void run() {
            try {
                for (int i = 0; i < tDuration; i++) {
                    if (isStop) {
                        Log.d(TAG, "节目切换 停止当前");
                        return;
                    }
                    countSec++;
                    Thread.sleep(1000);//线程暂停10秒，单位毫秒
                }
                if (isStop) {
                    Log.d(TAG, "节目切换 停止当前");
                    return;
                }
                Log.i(TAG, "结束播放webView" + mAdvItem.getResourceUrl());
                endTime = new Date();
                if (mListener != null) {
                    mListener.onPlayAdvItemResult(true, mAdvItem.getResourceId(), AdvConstants.RES_TYPE_IMAGE, countSec,
                            startTime, endTime, this.fragment);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                endTime = new Date();
                mListener.onPlayAdvItemResult(false, mAdvItem.getResourceId(), AdvConstants.RES_TYPE_IMAGE,
                        countSec, startTime, endTime, this.fragment);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}