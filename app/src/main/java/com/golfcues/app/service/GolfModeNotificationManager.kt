package com.golfcues.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.golfcues.app.MainActivity
import com.golfcues.app.R

class GolfModeNotificationManager(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "golf_mode_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.golfcues.app.ACTION_STOP_GOLF_MODE"
        const val ACTION_OPEN = "com.golfcues.app.ACTION_OPEN_APP"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_tap_hint)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(ACTION_STOP).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text))
            .setSubText(context.getString(R.string.notification_tap_hint))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(0, context.getString(R.string.action_stop_golf_mode), stopIntent)
            .addAction(0, context.getString(R.string.action_open_app), openIntent)
            .build()
    }
}
