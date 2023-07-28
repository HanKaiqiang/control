package com.touhuwai.hiadvbox;

import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.touhuwai.control.R;
import com.touhuwai.control.utils.DeviceInfoUtil;
import com.yanzhenjie.permission.AndPermission;

import java.io.File;
import java.util.Date;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VideoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoFragment extends Fragment {
    private static final String TAG = VideoFragment.class.getSimpleName();

    // TODO: Rename parameter arguments, choose names that match
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    VideoView vv1;


    HiAdvItem mAdvItem;
    IAdvPlayEventListener mListener;
    Date startTime;
    Date endTime;
    public int progress = 0;

    private ProgressBar progressBar;

    public VideoFragment(){

    }

    public VideoFragment(HiAdvItem item, IAdvPlayEventListener listener, int progress){
        mAdvItem = item;
        mListener = listener;
        progress = progress;
    }

    public static Fragment newInstance(HiAdvItem item, IAdvPlayEventListener listener, int progress) {
        return new VideoFragment(item,listener, progress);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment VideoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static VideoFragment newInstance(String param1, String param2) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


        if(AndPermission.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            //执行业务
            Log.i(TAG, "有文件读写权限");
        }else {
            //申请权限
            Log.w(TAG, "无文件读写权限");
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 在Fragment中这句话不能注释，否则Fragment接收不到获取权限的通知。
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private TextView wifiTextView;
    private Handler wifiHandler = new Handler();
    private Runnable wifiRssiRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                int rssi = DeviceInfoUtil.getRssi(getContext());
                String text = "rssi:" + rssi;
                wifiTextView.setText(text);
            } finally {
                wifiHandler.postDelayed(this, 5); // 10秒监测一次是否断连
            }
        }
    };

    @Override
    public void onDestroy() {
        wifiHandler.removeCallbacks(wifiRssiRunnable);
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.i(TAG, "onPause()");
        vv1.stopPlayback();
    }

    @Override
    public void onResume() {
        super.onResume();
        String localResourceFilePath = mAdvItem.getLocalResourceFilePath();
        Log.i(TAG, "开始播放视频：" + localResourceFilePath);
        if (vv1 == null) {
            Log.e(TAG, "videoView is null!");
        }
        if (!new File(localResourceFilePath).exists()) {
            Log.e(TAG, "要播放的文件不存在" + localResourceFilePath);
            startTime = new Date();
            endTime = startTime;
            mListener.onPlayAdvItemResult(false,
                    mAdvItem.getResourceId(),
                    AdvConstants.RES_TYPE_VIDEO,
                    (int) AdvDateUtil.diffSecond(startTime, endTime),
                    startTime,
                    endTime, null
            );
            return;
        }
        try {
            vv1.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "play video error");
                endTime = new Date();
                mListener.onPlayAdvItemResult(false,
                        mAdvItem.getResourceId(),
                        AdvConstants.RES_TYPE_VIDEO,
                        (int) AdvDateUtil.diffSecond(startTime, endTime),
                        startTime,
                        endTime, null
                );
                return true;
            });
            vv1.setVideoPath(localResourceFilePath);
            vv1.seekTo(0);
            vv1.start();
            startTime = new Date();
            vv1.setOnCompletionListener(mp -> {
                Log.i(TAG, "视频播放结束：" + localResourceFilePath);
                endTime = new Date();
                mListener.onPlayAdvItemResult(true,
                        mAdvItem.getResourceId(),
                        AdvConstants.RES_TYPE_VIDEO,
                        (int) AdvDateUtil.diffSecond(startTime, endTime),
                        startTime,
                        endTime, null
                );
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        progressBar = view.findViewById(R.id.vio_progress);
        progressBar.setProgress(progress);
        Log.i(TAG, "onCreateView()");
        vv1 = view.findViewById(R.id.vv1);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        wifiTextView = getView().findViewById(R.id.wifi_text_view);
        int rssi = DeviceInfoUtil.getRssi(getContext());
        String text = "rssi:" + rssi;
        wifiTextView.setText(text);
        wifiHandler.postDelayed(wifiRssiRunnable, 5); // 10秒监测一次是否断连

    }
}
