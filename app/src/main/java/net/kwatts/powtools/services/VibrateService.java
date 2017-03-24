package net.kwatts.powtools.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.os.*;
import org.greenrobot.eventbus.*;
import android.support.annotation.Nullable;

import net.kwatts.powtools.events.VibrateEvent;

public class VibrateService extends Service{
    private static final String TAG = "POWTOOLS";

    final int PAUSE_MS = 500;

    SharedPreferences mSharedPref;
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(final VibrateEvent event){
        Log.i(TAG, "Triggered Alarm: " + event.length + "/" + event.count);
        boolean doAlerts = mSharedPref.getBoolean("lowBatteryAlerts",false);
        if (doAlerts) {
            vibrate(event.length,event.count);
        }
    }
    private final IBinder mBinder = new MyBinder();
    public Vibrator mVibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);

    }

    public void vibrate(int length, int count) {
        final long[] pattern = { 0, length, PAUSE_MS };
        for (int i = 0; i < count; i++) {
            mVibrator.vibrate(pattern, -1);
            try {
                Thread.sleep(length + PAUSE_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Vibrate Service started");
        return Service.START_NOT_STICKY;
        //return super.onStartCommand(intent, flags, startId);

    }
    @Override
    public void onDestroy(){
        Log.v(TAG,"Vibrate Service killed");
        super.onDestroy();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //   return null;
        return mBinder;

    }
    public class MyBinder extends Binder {
        public VibrateService getService() {
            return VibrateService.this;
        }
    }

}