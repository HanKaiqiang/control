package com.touhuwai.hiadvbox;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;

public class AdvanceImageView extends RelativeLayout {
    private ImageView imageView;

    public Integer duration; // 播放时长

    public AdvanceImageView(Context context) {
        super(context);
        initView();
    }

    public AdvanceImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public AdvanceImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }


    private void initView() {
        imageView = new ImageView(getContext());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(imageView, new LayoutParams(-1, -1));
    }

    public void setImage(Object path) {
        Glide.with(getContext())
                .load(path)
                .transform(new GlideBitmapTransformation())
                .into(imageView);
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

}