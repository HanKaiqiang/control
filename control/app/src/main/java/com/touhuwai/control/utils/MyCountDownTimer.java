package com.touhuwai.control.utils;

import android.os.CountDownTimer;

public class MyCountDownTimer extends CountDownTimer {
    /**
     * @param millisInFuture
     *      表示以「 毫秒 」为单位倒计时的总数
     *      例如 millisInFuture = 1000 表示1秒
     *
     * @param countDownInterval
     *      表示 间隔 多少微秒 调用一次 onTick()
     *      例如: countDownInterval = 1000 ; 表示每 1000 毫秒调用一次 onTick()
     *
     */

    public MyCountDownTimer(long millisInFuture, long countDownInterval) {
        super(millisInFuture, countDownInterval);
    }


    public void onFinish() {
//        mCountDownTextView.setText("0s 跳过");
    }

    public void onTick(long millisUntilFinished) {
//        mCountDownTextView.setText( millisUntilFinished / 1000 + "s 跳过");
    }

}
