package net.kwatts.powtools.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import net.kwatts.powtools.MainActivity
import net.kwatts.powtools.R

fun Service.runAsForegroundService() {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.connection_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_wheel_connected)
        .setContentText(getString(R.string.connection_notification_text))
        .setContentIntent(pendingIntent)
        .build()
    startForeground(CONNECTION_NOTIFICATION_ID, notification)
}

fun Service.stopRunningAsForeground() {
    stopForeground(true)
}

private const val CHANNEL_ID = "connectionStatus"
private const val CONNECTION_NOTIFICATION_ID = 1
