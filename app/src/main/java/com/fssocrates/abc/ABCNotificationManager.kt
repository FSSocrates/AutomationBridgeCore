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
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_LOW, "ABC Engine", NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false); enableVibration(false); setSound(null, null)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_HIGH, "ABC Action Required", NotificationManager.IMPORTANCE_HIGH).apply {
                    setShowBadge(true); enableVibration(true)
                }
            )
        }
    }

    fun buildLowPriority(context: Context, jobId: String? = null): Notification {
        val text = jobId?.let { "Job $it running…" } ?: "Processing web workflows…"
        return NotificationCompat.Builder(context, CHANNEL_LOW)
            .setContentTitle("ABC Engine Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun buildHighPriority(context: Context, reason: String, jobId: String): Notification {
        val intent = Intent(context, SolverActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ABCForegroundService.EXTRA_JOB_ID, jobId)
            putExtra(ABCForegroundService.EXTRA_REASON, reason)
        }
        val pending = PendingIntent.getActivity(
            context, jobId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_HIGH)
            .setContentTitle("Action required")
            .setContentText("$reason — Job $jobId. Tap to continue.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
    }

    fun showLow(context: Context, jobId: String? = null) {
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID_LOW, buildLowPriority(context, jobId))
    }

    fun showHigh(context: Context, reason: String, jobId: String) {
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID_HIGH, buildHighPriority(context, reason, jobId))
    }

    fun cancelHigh(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID_HIGH)
    }
}
