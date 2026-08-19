package com.fssocrates.abc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object ABCNotificationManager {

    const val CHANNEL_LOW = "abc_engine_low"
    const val CHANNEL_HIGH = "abc_engine_high"
    const val NOTIFICATION_ID_LOW = 1001
    const val NOTIFICATION_ID_HIGH = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val low = NotificationChannel(
                CHANNEL_LOW,
                context.getString(R.string.notification_channel_low),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val high = NotificationChannel(
                CHANNEL_HIGH,
                context.getString(R.string.notification_channel_high),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(true)
                enableVibration(true)
            }
            nm.createNotificationChannel(low)
            nm.createNotificationChannel(high)
        }
    }

    fun buildLowPriority(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_LOW)
            .setContentTitle(context.getString(R.string.notification_title_active))
            .setContentText("Processing web workflows…")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun buildHighPriority(context: Context): Notification {
        val intent = Intent(context, SolverActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_HIGH)
            .setContentTitle(context.getString(R.string.notification_title_verify))
            .setContentText(context.getString(R.string.notification_text_verify))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pending)
            .setAutoCancel(true) // dismisses on click
            .build()
    }

    fun showLow(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID_LOW, buildLowPriority(context))
    }

    fun showHigh(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID_HIGH, buildHighPriority(context))
    }

    fun cancelHigh(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIFICATION_ID_HIGH)
    }
}
