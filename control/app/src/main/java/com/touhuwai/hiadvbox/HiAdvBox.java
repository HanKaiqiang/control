package com.touhuwai.hiadvbox;

import static com.touhuwai.control.db.DbHelper.FILE_DOWN_STATUS_SUCCESS;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.touhuwai.control.db.DbHelper;
import com.touhuwai.control.entry.FileDto;
import com.touhuwai.hiadvbox.pager.AccordionTransformer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 自定义广告播放控件
 */
public class HiAdvBox extends RelativeLayout implements IAdvPlayEventListener{
    private static final String TAG = HiAdvBox.class.getSimpleName();
    private AppCompatActivity mActivity;
    private SQLiteDatabase db;
    private FragmentStateAdapter adapter;
    private List<HiAdvItem> mAdvItemsList = new ArrayList<>();

    //外部监听器
    IAdvPlayEventListener mExternalEventListener;

    IAdvPlayEventListener endListener;

    private List<Fragment> fragmentList = new ArrayList<>();

    private int mPosition = 0;

    public int progress = 0;

    public HiAdvBox(Context context, IAdvPlayEventListener listener) {
        super(context);
        initView(context);
        this.endListener = listener;
        Log.i(TAG, "in HiAdvBox(Context context)...");
    }

    public HiAdvBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }


    private void setData(List<HiAdvItem> itemsList){
        mAdvItemsList.clear();
        mAdvItemsList = itemsList;
    }
    private TableLayout mTab_layout;
    private ViewPager2 mVp2;
    private RelativeLayout.LayoutParams mTableLayoutParams;
    private RelativeLayout.LayoutParams mVp2Params;

    private void initView(Context context){
        mTab_layout = new TableLayout(context);
        mVp2 = new ViewPager2(context);

        mTableLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        mTableLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        addView(mTab_layout, mTableLayoutParams);
        mVp2Params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        //mVp2Params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mVp2.setUserInputEnabled(false);
        addView(mVp2, mVp2Params);
    }

    public void init(AppCompatActivity activity, SQLiteDatabase db){
        mActivity = activity;
        this.db = db;

    }

    public void restartWork(List<HiAdvItem> itemsList){
        stopWork();
        setData(itemsList);
        startWork();
    }

    private Map<String, Fragment> fragmentMap = new HashMap<>();

    private void startWork() {
        if (mActivity == null) {
            Log.e(TAG, "assign activity with init(), please!");
            return;
        }
        mVp2.setAdapter(null);
        mVp2.setPageTransformer(new AccordionTransformer());
//        if (mAdvItemsList.size() > 0) {
//            mVp2.setOffscreenPageLimit(1);
//        }
        if (adapter == null) {
            adapter = new FragmentStateAdapter(mActivity) {
                @NonNull
                @Override
                public Fragment createFragment(int position) {
                    Log.i(TAG, "creating Fragment position=" + position);
                    Fragment frag = null;
                    String resourceId = null;
                    try {
                        int TOTALSIZE = mAdvItemsList.size();
                        if (TOTALSIZE == 0) {
                            frag = BlankFragment.newInstance();
                            return frag;
                        }
                        int index = position % TOTALSIZE;    //index从0开始到size-1
                        HiAdvItem item = mAdvItemsList.get(index);
                        Log.i(TAG, "creating item=" + item);
                        if (item != null) {
                            int resType = item.getResourceType();
                            resourceId = item.getResourceId();
                            String localResourceFilePath = item.getLocalResourceFilePath();
                            //如果localResourceFilePath是空时查询数据库中是否异步重新下载成功
                            if (localResourceFilePath == null || localResourceFilePath.equals("null")) {
                                String resourceUrl = item.getResourceUrl();
                                FileDto fileDto = DbHelper.queryByUrl(db, resourceUrl);
                                if (Objects.equals(fileDto.status, FILE_DOWN_STATUS_SUCCESS)) {
                                    item.setLocalResourceFilePath(fileDto.path);
                                }
                            }
                            Fragment fragment = fragmentMap.get(resourceId);
                            if (fragment != null) {
                                return fragment;
                            }
                            switch (resType) {
                                case 0://图片
                                    Log.i(TAG, "creating an image frag");
                                    frag = ImageFragment.newInstance(item, endListener, progress);
                                    break;
                                case 1://视频
                                    Log.i(TAG, "creating a video frag");
                                    frag = VideoFragment.newInstance(item, endListener, progress);
                                    break;
                                default:
                                    Log.w(TAG, "unexpected resType!");
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, e.getMessage(), e);
                    }
                    if (frag == null) {
                        frag = BlankFragment.newInstance();
                    }
                    fragmentList.add(frag);
                    fragmentMap.put(resourceId, frag);
                    return frag;
                }

                @Override
                public int getItemCount() {
                    return Integer.MAX_VALUE;
                }

                @Override
                public boolean containsItem(long itemId) {
                    return super.containsItem(itemId);
                }
            };
        }
        mVp2.setAdapter(adapter);
        mActivity.runOnUiThread(() -> mVp2.setCurrentItem(mPosition, false));
    }

    public void stopWork(){
        if (!fragmentList.isEmpty()) {
            for (Fragment fragment : fragmentList) {
                if (fragment instanceof VideoFragment) {
                    if (((VideoFragment) fragment).vv1 != null) {
                        ((VideoFragment) fragment).vv1.stopPlayback();
                    }
                }
                if (fragment instanceof ImageFragment) {
                    ((ImageFragment) fragment).isStop = true;
                }
            }
            fragmentList.clear();
            fragmentMap.clear();
        }
        mPosition = 0;
        mVp2.setAdapter(null);
    }


    @Override
    public void onPlayAdvItemResult(boolean isSucceed, String resourceId, int resourceType, int actualDuration, Date startTime, Date endTime, ImageFragment fragment) {
        Log.i(TAG, "播放一条 item played. resourceType=" + resourceType
                + ", actualDuration=" + actualDuration
                + ", startTime=" + startTime
                + ", endTime=" + endTime
        );

        if (fragment != null && fragment.isStop) {
            Log.e(TAG, "节目切换 停止当前");
            return;
        }

        //外部回调，供外部使用
        if (mExternalEventListener != null) {
            mExternalEventListener.onPlayAdvItemResult(isSucceed, resourceId, resourceType, actualDuration, startTime, endTime, null);
        }
        int position = mVp2.getCurrentItem() + 1;
        mPosition++;
        if (position == mAdvItemsList.size()) {
            position = 0;
        }
        int finalPosition = position;
        mActivity.runOnUiThread(() -> mVp2.setCurrentItem(finalPosition, false));

    }

    public void setCurrentItem(Integer position){
        mActivity.runOnUiThread(() -> mVp2.setCurrentItem(position, false));
    }
}
