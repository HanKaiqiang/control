package com.touhuwai.hiadvbox;


import static com.touhuwai.hiadvbox.Advance.DEFAULT_DURATION;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.touhuwai.hiadvbox.pager.MyPageTransformer;

import java.util.ArrayList;
import java.util.List;

public class AdvancePagerAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {
    private Context context;
    private ViewPager viewPager;
    private List<HiAdvItem> datas;
    private List<View> list = new ArrayList<>();

    private int current = 0;
    private boolean pause;
    private List<Thread> threadList = new ArrayList<>();

    private int lastPosition = -1;

    private IAdvPlayEventListener endListener;

    public AdvancePagerAdapter(Context context, ViewPager viewPager, IAdvPlayEventListener endListener) {
        this.context = context;
        this.viewPager = viewPager;
        this.endListener = endListener;
    }


    public void setData(List<HiAdvItem> imageList) {
        if (imageList.size() == 0) return;
        this.datas = imageList;
        list.clear();
        addView(imageList.get(imageList.size() - 1));
        if (imageList.size() > 1) { //多于1个要循环
            for (HiAdvItem d : imageList) { //中间的N个（index:1~N）
                addView(d);
            }
            addView(imageList.get(0));
        }
        viewPager.addOnPageChangeListener(this);
        notifyDataSetChanged();
        viewPager.setPageTransformer(true, new MyPageTransformer());
        //在外层，将mViewPager初始位置设置为1即可
        if (imageList.size() > 1) { //多于1个，才循环并开启定时器
            viewPager.setCurrentItem(1);
            startTimer();
        }

    }

    private void addView(HiAdvItem advance) {
        AdvanceImageView imageView = new AdvanceImageView(context);
        imageView.setImage(advance.getLocalResourceFilePath());
        imageView.setDuration(advance.getResourceDuration());
        list.add(imageView);
    }

    private void startTimer() {
        for (Thread thread : threadList) {
            thread.interrupt();
        }
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && list.size() > 0) {
                try {
                    Thread lastThread = threadList.get(threadList.size() - 1);
                    if (Thread.currentThread().getId() != lastThread.getId()) {
                        threadList.remove(Thread.currentThread());
                        break;
                    }
                    Thread.sleep(1000);

                    View view = list.get(viewPager.getCurrentItem());
                    Integer duration = DEFAULT_DURATION;
                    if (view instanceof AdvanceImageView) {
                        current += 1000;
                        duration = ((AdvanceImageView) view).duration * 1000;
                    }
                    if (current >= duration) {
                        endListener.onPlayAdvItemResult(
                                true,
                                "1",
                                AdvConstants.RES_TYPE_IMAGE,
                                duration,
                                null,
                                null
                        );
//                        viewPager.post(() -> viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true));
                        current = 0;
                    }
                } catch (InterruptedException e) {
                    Log.e("AdvancePagerAdapter", e.getMessage(), e);
                }
            }
        });
        thread.start();
        threadList.add(thread);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(list.get(position));
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = list.get(position);
        container.addView(view);
        return view;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    //
//    // 实现ViewPager.OnPageChangeListener接口
    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
                               int positionOffsetPixels) {
        // 什么都不干
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == 0) {
            if (list.size() > 1) { //多于1，才会循环跳转
                if (viewPager.getCurrentItem() < 1) { //首位之前，跳转到末尾（N）
                    int position = datas.size(); //注意这里是mList，而不是mViews
                    viewPager.setCurrentItem(position, false);
                } else if (viewPager.getCurrentItem() > datas.size()) { //末位之后，跳转到首位（1）
                    viewPager.setCurrentItem(1, false); //false:不显示跳转过程的动画
                }
                current = 0;//换页重新计算时间
                lastPosition = viewPager.getCurrentItem();
            }
        }
    }

    public void setPause() {
        pause = true;
    }

    public void setResume() {
        pause = false;
    }
}
