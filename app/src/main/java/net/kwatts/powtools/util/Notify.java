package net.kwatts.powtools.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.Color;
import android.location.Address;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import net.kwatts.powtools.MainActivity;
import net.kwatts.powtools.R;

import timber.log.Timber;

public class Notify {
    private static final int    OW_STATUS_ID = 101;
    private static final String OW_STATUS_CN = "Onewheel Status";
    private static final String OW_STATUS_CD = "Show status updates";

    private static final int    REMAINING_ID = 102;
    private static final String REMAINING_CN = "Battery Remaining";
    private static final String REMAINING_CD = "Show battery remaining";

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

    // Breaking these up allows for individual notify settings
    private NotificationCompat.Builder notifyStatus;
    private NotificationCompat.Builder notifyRemain;
    private NotificationCompat.Builder notifyAlert75;
    private NotificationCompat.Builder notifyAlert50;
    private NotificationCompat.Builder notifyAlert25;
    private NotificationCompat.Builder notifyAlert05;

    private NotificationManagerCompat  notifyManager;

    private android.os.Handler clearHandler;
    private Context mContext;

    private int lastPercent = -1;
    private boolean alert75 = true;
    private boolean alert50 = true;
    private boolean alert25 = true;
    private boolean alert05 = true;

    public void init(MainActivity mainActivity) {
        mContext = mainActivity.getApplicationContext();
        clearHandler = new Handler(Looper.getMainLooper());

        createNotificationChannels();
        notifyManager = NotificationManagerCompat.from(mContext);
        startStatusNotification();
        waiting();
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

            channel = new NotificationChannel(OW_STATUS_ID+"", OW_STATUS_CN, importance);
            channel.setDescription(OW_STATUS_CD);
            notificationManager.createNotificationChannel(channel);

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
        notifyStatus = new NotificationCompat.Builder(mContext, OW_STATUS_ID+"")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Onewheel Status")
                        .setColor(Color.parseColor("#fcb103"))
                        .setContentText("Waiting for connection...")
                        .setOngoing(true)
                        .setAutoCancel(true);

        notifyRemain = new NotificationCompat.Builder(mContext, REMAINING_ID+"")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Battery:")
                        .setColor(Color.parseColor("#fcb103"))
                        .setContentText("-%")
                        .setOngoing(true)
                        .setAutoCancel(true);
        notifyRemain.setProgress(100, 0, false);

        notifyAlert75 = new NotificationCompat.Builder(mContext, ALERT_75_ID+"")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Onewheel Alerts")
                        .setColor(Color.parseColor("#fcb103"))
                        .setContentText(ALERT_75_CN)
                        .setOngoing(false)
                        .setAutoCancel(true);

        notifyAlert50 = new NotificationCompat.Builder(mContext, ALERT_50_ID+"")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Onewheel Alerts")
                        .setColor(Color.parseColor("#fcb103"))
                        .setContentText(ALERT_50_CN)
                        .setOngoing(false)
                        .setAutoCancel(true);

        notifyAlert25 = new NotificationCompat.Builder(mContext, ALERT_25_ID+"")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Onewheel Alerts")
                        .setColor(Color.parseColor("#fcb103"))
                        .setContentText(ALERT_25_CN)
                        .setOngoing(false)
                        .setAutoCancel(true);

        notifyAlert05 = new NotificationCompat.Builder(mContext, ALERT_05_ID+"")
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

    public void status(String title, String message) {
        notifyStatus.setContentText(title + ": " + message);
        notifyManager.notify(OW_STATUS_ID, notifyStatus.build());
    }

    public void waiting() {
        notifyStatus.setContentText("Waiting for connection...");
        notifyManager.notify(REMAINING_ID, notifyStatus.build());
        alert75 = true;
        alert50 = true;
        alert25 = true;
        alert05 = true;
    }

    public void remain(int percent) {
        notifyRemain.setContentText(percent+"%");
        notifyRemain.setProgress(100, percent, false);
        notifyManager.notify(REMAINING_ID, notifyRemain.build());
    }

    public void alert(int percent) {
        final int clear_id;

        if (alert05 && lastPercent > 5 && percent <= 5) {
            notifyManager.notify(ALERT_05_ID, notifyAlert05.build());
            clear_id = ALERT_05_ID;
            alert05 = false;
        } else if (alert25 && lastPercent > 25 && percent <= 25) {
            notifyManager.notify(ALERT_25_ID, notifyAlert25.build());
            clear_id = ALERT_25_ID;
            alert25 = false;
        } else if (alert50 && lastPercent > 50 && percent <= 50) {
            notifyManager.notify(ALERT_50_ID, notifyAlert50.build());
            clear_id = ALERT_50_ID;
            alert50 = false;
        } else if (alert75 && lastPercent > 75 && percent <= 75) {
            notifyManager.notify(ALERT_75_ID, notifyAlert75.build());
            clear_id = ALERT_75_ID;
            alert75 = false;
        } else {
            clear_id = -1;
        }

        lastPercent = percent;

        if (clear_id > 0) {
            // These don't really need to stick around, cancel after 10s
            clearHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyManager.cancel(clear_id);
                }
            }, 10000);
        }
    }

}
