package com.touhuwai.hiadvbox;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.touhuwai.control.R;
import com.touhuwai.control.utils.DeviceInfoUtil;


public class BlankFragment extends Fragment {

    public static Fragment newInstance() {
        return new BlankFragment();
    }
    private TextView wifiTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_blank,null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        wifiTextView = getView().findViewById(R.id.wifi_text_view);
        int rssi = DeviceInfoUtil.getRssi(getContext());
        String text = "rssi:" + rssi;
        wifiTextView.setText(text);
        wifiHandler.postDelayed(wifiRssiRunnable, 5); // 10秒监测一次是否断连
    }
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        LinearLayout linearLayout = getView().findViewById(R.id.ll_view);

    }

    @Override
    public void onDestroy() {
        wifiHandler.removeCallbacks(wifiRssiRunnable);
        super.onDestroy();
    }
}
