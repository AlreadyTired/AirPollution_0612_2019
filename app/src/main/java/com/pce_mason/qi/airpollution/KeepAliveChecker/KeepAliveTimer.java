package com.pce_mason.qi.airpollution.KeepAliveChecker;

import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.pce_mason.qi.airpollution.AppClientHeader.CustomTimer;

import java.util.Timer;
import java.util.TimerTask;

public class KeepAliveTimer {
    Context context;
    FragmentManager fragmentManager;
    Timer kasTimer, kasGraceTimer;
    int KAS_TIME = CustomTimer.T252;
    int GRACE_TIME = CustomTimer.KAS_GRACE_TIME;
    int KAS_SHOW_TIME = KAS_TIME - GRACE_TIME;


    public KeepAliveTimer(Context context, FragmentManager fragmentManager){
        this.fragmentManager = fragmentManager;
        this.context = context;
    }

    public TimerTask KeepAliveDialogTimerTaskMaker(){
        TimerTask keepAliveTimerTask = new TimerTask() {
            @Override
            public void run(){
                CheckerDialogFragment checkerDialogFragment = new CheckerDialogFragment();
                checkerDialogFragment.show(fragmentManager,"Keep Alive Dialog");
                stopKasTimer();

                startKasGraceTimer();
            }
        };
        return keepAliveTimerTask;
    }
    public TimerTask KeepAliveTimerTaskMaker(){
        TimerTask keepAliveTimerTask = new TimerTask() {
            @Override
            public void run(){
                stopKasGraceTimer();
                //System.exit(1);
            }
        };
        return keepAliveTimerTask;
    }

    public void startKasTimer(){
        kasTimer = new Timer();
        kasTimer.schedule(KeepAliveDialogTimerTaskMaker(), KAS_SHOW_TIME, KAS_SHOW_TIME);
        Log.d("CLICK",String.valueOf(KAS_SHOW_TIME));
    }
    public void startKasGraceTimer(){
        kasGraceTimer = new Timer();
        kasGraceTimer.schedule(KeepAliveTimerTaskMaker(),GRACE_TIME);
    }

    public void stopKasTimer(){
        if (kasTimer != null) {
            kasTimer.cancel();
            kasTimer = null;
        }
    }
    public void stopKasGraceTimer(){
        if (kasGraceTimer != null){
            kasGraceTimer.cancel();
            kasGraceTimer = null;
        }
    }

    public void stopAllTimer(){
        stopKasGraceTimer();
        stopKasTimer();
    }
}
