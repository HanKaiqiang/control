package com.touhuwai.hiadvbox;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.touhuwai.control.R;
import com.touhuwai.control.utils.DeviceInfoUtil;

import java.util.Date;



public class ImageFragment extends Fragment {
    private static final String TAG = ImageFragment.class.getSimpleName();
    private ProgressBar progressBar;
    ImageView iv_pic;

    IAdvPlayEventListener mListener;

    HiAdvItem mAdvItem;

    Date startTime;
    Date endTime;
    protected boolean isStop;

    public int progress = 0;

    public static synchronized Fragment newInstance(HiAdvItem advItem,
                                                    IAdvPlayEventListener listener, int progress) {
        return new ImageFragment(advItem, listener, progress);
    }

    public ImageFragment(HiAdvItem advItem,
                         IAdvPlayEventListener listener, int progress) {
        mAdvItem = advItem;
        mListener = listener;
        this.progress = progress;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Log.i(TAG, "onCreateView, imageUrl=" + mAdvItem.getLocalResourceFilePath());
        View view = inflater.inflate(R.layout.fragment_image, container, false);
        progressBar = view.findViewById(R.id.img_progress);
        progressBar.setProgress(progress);
        iv_pic = view.findViewById(R.id.iv_pic);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        wifiTextView = getView().findViewById(R.id.wifi_text_view);
        String text = "rssi:" + DeviceInfoUtil.getRssi(getContext());
        wifiTextView.setText(text);
        wifiHandler.postDelayed(wifiRssiRunnable, 5);
        String imageUrl = mAdvItem.getLocalResourceFilePath();
        if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageUrl, options);
            if (options.outWidth == -1) {
                new Thread(new MyThread(0, this)).start();
            } else {
                startTime = new Date();
                Glide.with(this)
                        .load(imageUrl)
                        .transform(new GlideBitmapTransformation())
                        .error(R.drawable.img)
                        .into(iv_pic);
                Log.i(TAG, "开始播放图片" + imageUrl);
            }
        } else {
            new Thread(new MyThread(0, this)).start();
        }
    }
    private TextView wifiTextView;
    private Handler wifiHandler = new Handler();
    private Runnable wifiRssiRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                String text = "rssi:" + DeviceInfoUtil.getRssi(getContext());
                wifiTextView.setText(text);
            } finally {
                wifiHandler.postDelayed(this, 1000);
            }
        }
    };

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

    public class MyThread implements Runnable{
        private int tDuration = 5;

        private ImageFragment fragment;

        public MyThread(int duration, ImageFragment fragment) {
            tDuration = duration;
            this.fragment = fragment;
        }

        int countSec = 0;

        @Override
        public void run() {
            try {
                //int countSec = 0;
                for(int i=0; i<tDuration; i++) {
                    if (isStop) {
                        Log.d(TAG, "节目切换 停止当前");
                        return;
                    }
                    countSec ++;
                    Thread.sleep(1000);//线程暂停10秒，单位毫秒
                }
                if (isStop) {
                    Log.d(TAG, "节目切换 停止当前");
                    return;
                }
                Log.i(TAG, "结束播放图片" + mAdvItem.getResourceUrl());
                endTime = new Date();
                if (mListener != null) {
                    mListener.onPlayAdvItemResult( true, mAdvItem.getResourceId(), AdvConstants.RES_TYPE_IMAGE, countSec,
                            startTime,  endTime, this.fragment);
                }
//                mListener = null;
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
        wifiHandler.removeCallbacks(wifiRssiRunnable);
        super.onDestroy();
    }
}