package com.ipaulpro.afilechooserexample;

import android.os.CountDownTimer;
import android.os.Handler;

public class Utils {

    public interface DelayCallback{
        void afterDelay();
    }

    public static void delay(int msecs, final DelayCallback delayCallback){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                delayCallback.afterDelay();
            }
        }, msecs);
    }
    public static void delay(int period, int tic){
        new CountDownTimer(period, tic) {

            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
            }
        }.start();
    }
}