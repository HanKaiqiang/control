package com.touhuwai.hiadvbox;

import static com.touhuwai.control.db.DbHelper.FILE_DOWN_STATUS_SUCCESS;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.touhuwai.control.R;
import com.touhuwai.control.db.DbHelper;
import com.touhuwai.control.entry.FileDto;
import com.touhuwai.hiadvbox.pager.AccordionTransformer;

/**
 * 自定义广告播放控件
 */
public class HiAdvBox extends RelativeLayout implements IAdvPlayEventListener{
    private static final String TAG = HiAdvBox.class.getSimpleName();
    private Context mContext = null;
    private AppCompatActivity mActivity;
    private AdvWorker mWorker;

    private SQLiteDatabase db;
    private List<HiAdvItem> mAdvItemsList = new ArrayList<>();

    //外部监听器
    IAdvPlayEventListener mExternalEventListener;

    //展示下一屏资源
    private static final int MSG_DISPLAY_START = 0;
    private static final int MSG_DISPLAY_NEXT = 1;

    private int mPosition = 0;

    public HiAdvBox(Context context) {
        super(context);
        Log.i(TAG, "in HiAdvBox(Context context)...");
    }

    public HiAdvBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i(TAG, "HiAdvBox(Context context, AttributeSet attrs)...");

        //initAttr(context, attrs);
        initView(context);
        //initEvent();
    }


    private void setData(List<HiAdvItem> itemsList){
        mAdvItemsList = itemsList;
    }

    private TextView mTitle;
    private TableLayout mTab_layout;
    private ViewPager2 mVp2;
    private RelativeLayout.LayoutParams mTitleParams;
    private RelativeLayout.LayoutParams mTableLayoutParams;
    private RelativeLayout.LayoutParams mVp2Params;

    private void initView(Context context){
        mContext = context;

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

    public void setAdvEventListener(IAdvPlayEventListener listener){
        mExternalEventListener = listener;
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

    private void startWork(){
        //mHandler = new MyHandler();
        if(mActivity==null){
            Log.e(TAG, "assign activity with init(), please!");
            return;
        }
        //mWorker = new AdvWorker(mVp2);
        mVp2.setAdapter(null);
        mVp2.setPageTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
//                page.setScaleX(0.75f);
//                page.setScaleY(0.75f);
//                page.setAlpha(0.5f);
            }
        });
        mVp2.setAdapter(new FragmentStateAdapter(mActivity) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                Log.i(TAG, "creating Fragment position=" + position);
                //return null;
                Fragment frag = null;
                try {
                    int TOTALSIZE = mAdvItemsList.size();
                    if(TOTALSIZE==0){
                        frag = BlankFragment.newInstance();
                        return frag;
                    }
                    int index = position % TOTALSIZE;    //index从0开始到size-1
                    HiAdvItem item = mAdvItemsList.get(index);
                    Log.i(TAG, "creating item=" + item);
                    if (item != null){
                        int resType = item.getResourceType();
                        //String ruleDetailId = ruleDetail.getRuleDetailId();
                        int duration = item.getResourceDuration()==0? 5: item.getResourceDuration();

//                        String localResPath = AdvConstants.FILE_PATH + AdvMyStringUtil.getFileNameFromUrl(item.getResourceUrl());
                        String localResourceFilePath = item.getLocalResourceFilePath();
                        //如果localResourceFilePath是空时查询数据库中是否异步重新下载成功
                        if(localResourceFilePath == null || localResourceFilePath.equals(String.valueOf(R.drawable.img))){
                            String resourceUrl = item.getResourceUrl();
                            FileDto fileDto = DbHelper.queryByUrl(db, resourceUrl);
                            if (Objects.equals(fileDto.status, FILE_DOWN_STATUS_SUCCESS)) {
                                item.setLocalResourceFilePath(fileDto.path);
                            }
                        }
                        switch (resType){
                            case 0://图片
                                Log.i(TAG, "creating an image frag");
                                frag = ImageFragment.newInstance(item, HiAdvBox.this);
                                break;
                            case 1://视频
                                Log.i(TAG, "creating a video frag");
                                frag = VideoFragment.newInstance(item, HiAdvBox.this);
                                break;
                            default:
                                Log.w(TAG, "unexpected resType!");
                                break;
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    Log.w(TAG, "unexpected error!");
                }
                if(frag==null) {
                    frag = BlankFragment.newInstance();
                }
                return frag;
            }

            @Override
            public int getItemCount() {
                /**
                 * 按5秒播放一个广告计算，可以连续播放340年
                 */
                return Integer.MAX_VALUE;
            }

            @Override
            public boolean containsItem(long itemId) {
                return super.containsItem(itemId);
            }
        });
    }

    private void pauseWork(){

    }

    private void resumeWork(){

    }

    public void stopWork(){
        mPosition = 0;
        mWorker = null;
        mVp2.setAdapter(null);
    }


    @Override
    public void onPlayAdvItemResult(boolean isSucceed, String resourceId, int resourceType, int actualDuration, Date startTime, Date endTime) {
        Log.i(TAG, "播放一条 item played. resourceType=" + resourceType
        + ", actualDuration=" + actualDuration
                + ", startTime=" + startTime
                + ", endTime=" + endTime
        );

        //外部回调，供外部使用
        if(mExternalEventListener!=null){
            mExternalEventListener.onPlayAdvItemResult(isSucceed, resourceId, resourceType, actualDuration, startTime, endTime);
        }

        if(isSucceed){
            //TODO: 记录播放日志并择机上传
        }

        mPosition++;
        mActivity.runOnUiThread(() -> mVp2.setCurrentItem(mPosition, false));

    }
}
