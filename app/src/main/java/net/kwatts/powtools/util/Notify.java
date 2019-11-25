package net.kwatts.powtools.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.Color;
import android.location.Address;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v4.content.ContextCompat;

import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.R;

import timber.log.Timber;

public class Notify {
    private static final String TAG_STATUS   = "powStatusNotificationTag";
    private static final int    REMAINING_ID = 101;
    private static final String REMAINING_CN = "Battery Status";
    private static final String REMAINING_CD = "Show battery status for Onewheel";

    private static final String TAG_ALERT    = "powAlertNotificationTag";
    private static final String ALERT_GR_KEY = "pow_alert_group";
    private static final int    ALERT_75_ID  = 201;
    private static final String ALERT_75_CN  = "Battery 75% Remaining";
    private static final String ALERT_75_CD  = "Alarm when battery is 75%";

    private static final int    ALERT_50_ID  = 202;
    private static final String ALERT_50_CN  = "Battery 50% Remaining";
    private static final String ALERT_50_CD  = "Alarm when battery is 50%";

    private static final int    ALERT_25_ID  = 203;
    private static final String ALERT_25_CN  = "Battery 25% Remaining";
    private static final String ALERT_25_CD  = "Alarm when battery is 25%";

    private static final int    ALERT_05_ID  = 204;
    private static final String ALERT_05_CN  = "Battery 5% Remaining";
    private static final String ALERT_05_CD  = "Alarm when battery is 5%";

    private NotificationCompat.Builder notifyStatus;
    private NotificationCompat.Builder notifyAlert75;
    private NotificationCompat.Builder notifyAlert50;
    private NotificationCompat.Builder notifyAlert25;
    private NotificationCompat.Builder notifyAlert05;

    private NotificationManagerCompat  notifyManager;

    private android.os.Handler clearHandler;
    private Context mContext;

    
    public void init(MainActivity mainActivity) {
        mContext = mainActivity.getApplicationContext();
        clearHandler = new Handler(Looper.getMainLooper());

        createNotificationChannels();
        notifyManager = NotificationManagerCompat.from(mContext);
        startStatusNotification();
    }


    private void createNotificationChannels() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance;
            NotificationChannel channel;
            NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);

            //Status
            importance = NotificationManager.IMPORTANCE_DEFAULT;

            channel = new NotificationChannel(REMAINING_ID+"", REMAINING_CN, importance);
            channel.setDescription(REMAINING_CD);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);

            //Alert
            importance = NotificationManager.IMPORTANCE_HIGH;

            channel = new NotificationChannel(ALERT_75_ID+"", ALERT_75_CN, importance);
            channel.setDescription(ALERT_75_CD);
            channel.setVibrationPattern(new long[]{0,500});
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);

            channel = new NotificationChannel(ALERT_50_ID+"", ALERT_50_CN, importance);
            channel.setDescription(ALERT_50_CD);
            channel.setVibrationPattern(new long[]{0,500,500,500});
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);

            channel = new NotificationChannel(ALERT_25_ID+"", ALERT_25_CN, importance);
            channel.setDescription(ALERT_25_CD);
            channel.setVibrationPattern(new long[]{0,500,500,500,500,500});
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);

            channel = new NotificationChannel(ALERT_05_ID+"", ALERT_05_CN, importance);
            channel.setDescription(ALERT_05_CD);
            channel.setVibrationPattern(new long[]{0,500,500,500,500,500,500,500});
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startStatusNotification() {
        notifyStatus = new NotificationCompat.Builder(mContext, REMAINING_ID+"")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Onewheel Status")
                        .setColor(Color.parseColor("#fcb103"))
                        .setContentText("Waiting for connection...")
                        .setOngoing(true)
                        .setAutoCancel(true);
        notifyManager.notify(REMAINING_ID, notifyStatus.build());

        notifyAlert75 = new NotificationCompat.Builder(mContext, ALERT_75_ID+"")
                        .setGroup(ALERT_GR_KEY)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Onewheel Alerts")
                        .setColor(Color.parseColor("#fcb103"))
                        .setContentText(ALERT_75_CN)
                        .setOngoing(false)
                        .setAutoCancel(true);

        notifyAlert50 = new NotificationCompat.Builder(mContext, ALERT_50_ID+"")
                        .setGroup(ALERT_GR_KEY)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Onewheel Alerts")
                        .setColor(Color.parseColor("#fcb103"))
                        .setContentText(ALERT_50_CN)
                        .setOngoing(false)
                        .setAutoCancel(true);

        notifyAlert25 = new NotificationCompat.Builder(mContext, ALERT_25_ID+"")
                        .setGroup(ALERT_GR_KEY)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Onewheel Alerts")
                        .setColor(Color.parseColor("#fcb103"))
                        .setContentText(ALERT_25_CN)
                        .setOngoing(false)
                        .setAutoCancel(true);

        notifyAlert05 = new NotificationCompat.Builder(mContext, ALERT_05_ID+"")
                        .setGroup(ALERT_GR_KEY)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Onewheel Alerts")
                        .setColor(Color.parseColor("#fcb103"))
                        .setContentText(ALERT_05_CN)
                        .setOngoing(false)
                        .setAutoCancel(true);
    }


    public void stopStatusNotification() {
	   notifyManager.cancelAll();
    }

    public void event(String title, String message) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext, "ponewheel")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setColor(0x008000)
                        .setContentText(message);

        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(contentIntent);

        notifyManager.notify(message, 1, mBuilder.build());
    }

    public void status(int percent) {
        notifyStatus.setContentText(percent+"%");
        notifyManager.notify(REMAINING_ID, notifyStatus.build());
    }

    public void alert(int percent) {
        final int alert_id;
        NotificationCompat.Builder notify_alert = notifyStatus;

        switch(percent) {
            case 75:
                alert_id = ALERT_75_ID;
                notify_alert = notifyAlert75;
                break;
            case 50:
                alert_id = ALERT_50_ID;
                notify_alert = notifyAlert50;
                break;
            case 25:
                alert_id = ALERT_25_ID;
                notify_alert = notifyAlert25;
                break;
            case 5:
                alert_id = ALERT_05_ID;
                notify_alert = notifyAlert05;
                break;
            default:
                alert_id = 0;
                Timber.d("alert percentage:" + percent);
        }

        if (alert_id > 0) {
            notifyManager.notify(alert_id, notify_alert.build());

            clearHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyManager.cancel(alert_id);
                }
            }, 10000);
        }
    }

}
