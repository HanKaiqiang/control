package com.touhuwai.hiadvbox;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

public class AdvanceView extends RelativeLayout {
    private ViewPager viewPager;
    private List<View> views = new ArrayList<>();
    private AdvancePagerAdapter adapter;

    private IAdvPlayEventListener endListener;

    public AdvanceView(Context context, IAdvPlayEventListener listener) {
        super(context);
        endListener = listener;
        initView();
    }

    public AdvanceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public AdvanceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        viewPager = new ViewPager(getContext());
        adapter = new AdvancePagerAdapter(getContext(), viewPager, endListener);
        viewPager.setAdapter(adapter);
        addView(viewPager, new LayoutParams(-1, -1));
    }

    public void setData(List<HiAdvItem> imageList) {
        adapter.setData(imageList);
    }

    public void setCurrentItem(Integer position){
        viewPager.post(() -> viewPager.setCurrentItem(position, true));
    }

    public void setPause(){
        adapter.setPause();
    }
    public void setResume(){
        adapter.setResume();
    }
}