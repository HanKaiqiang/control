package com.touhuwai.hiadvbox;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TableLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.touhuwai.control.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Control extends RelativeLayout implements IAdvPlayEventListener{

    private static final String TAG = Control.class.getSimpleName();
    private AppCompatActivity mActivity;
    private SQLiteDatabase db;
    private List<HiAdvItem> mAdvItemsList = new ArrayList<>();
    public int progress = 0;

    HiAdvBox hiAdvBox;
    AdvanceView advanceView;

    public void init(AppCompatActivity activity, SQLiteDatabase db, HiAdvBox hiAdvBox, AdvanceView advanceView){
        mActivity = activity;
        this.db = db;
        this.hiAdvBox = hiAdvBox;
        this.advanceView = advanceView;
        hiAdvBox.init(mActivity, db, this);
        advanceView.initView(this);
    }

    private void setData(List<HiAdvItem> itemsList){
        mAdvItemsList.clear();
        mAdvItemsList = itemsList;
    }

    private Map<Integer, Integer> imagePositionMap = new HashMap<>();
    private Map<Integer, Integer> videoPositionMap = new HashMap<>();

    public void restartWork(List<HiAdvItem> itemsList){
        setData(itemsList);
        advanceView.setPause();
        hiAdvBox.stopWork();
        List<HiAdvItem> imageList = new ArrayList<>();
        List<HiAdvItem> videoList = new ArrayList<>();
        for (int i = 0; i < itemsList.size(); i++) {
            HiAdvItem hiAdvItem = itemsList.get(i);
            int resourceType = hiAdvItem.getResourceType();
            if (resourceType == 0) {
                imagePositionMap.put(i, imageList.size());
                imageList.add(hiAdvItem);
            }
            if (resourceType == 1) {
                videoPositionMap.put(i, videoList.size());
                videoList.add(hiAdvItem);
            }
        }
        advanceView.setData(imageList);
        hiAdvBox.restartWork(videoList);
    }

    private int mPosition = 0;
    @Override
    public void onPlayAdvItemResult(boolean isSucceed, String resourceId, int resourceType,
                                    int actualDuration, Date startTime, Date endTime,
                                    ImageFragment fragment) {
        Log.i(TAG, "播放一条 item played. resourceType=" + resourceType
                + ", actualDuration=" + actualDuration
                + ", startTime=" + startTime
                + ", endTime=" + endTime
        );

        mPosition++;
        if (mPosition == mAdvItemsList.size()) {
            mPosition = 0;
        }
        Integer imagePosition = imagePositionMap.get(mPosition);
        Integer videoPosition = videoPositionMap.get(mPosition);
        if (imagePosition != null) {
            advanceView.setCurrentItem(mPosition);
        }
        if (videoPosition != null) {
            hiAdvBox.setCurrentItem(mPosition);
        }
    }


    public Control(Context context) {
        super(context);
    }

    public Control(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Control(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
