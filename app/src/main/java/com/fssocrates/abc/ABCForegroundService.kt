package com.fssocrates.abc

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationJob
import timber.log.Timber

/** Android host for AutomationCoordinator. IPC adapter only. */
class ABCForegroundService : Service() {

    companion object {
        const val ACTION_START_JOB = "com.fssocrates.abc.ACTION_START_JOB"
        const val ACTION_CANCEL_JOB = "com.fssocrates.abc.ACTION_CANCEL_JOB"
        const val EXTRA_JOB_ID = "com.fssocrates.abc.EXTRA_JOB_ID"
        const val EXTRA_TARGET_URL = "com.fssocrates.abc.EXTRA_TARGET_URL"
        const val EXTRA_SCRIPT = "com.fssocrates.abc.EXTRA_SCRIPT"
        const val ACTION_LINK_EXTRACTED = "com.fssocrates.abc.ACTION_LINK_EXTRACTED"
        const val ACTION_JOB_EVENT = "com.fssocrates.abc.ACTION_JOB_EVENT"
        const val EXTRA_RESULT_URL = "com.fssocrates.abc.EXTRA_RESULT_URL"
        const val EXTRA_EVENT = "com.fssocrates.abc.EXTRA_EVENT"
        const val EXTRA_REASON = "com.fssocrates.abc.EXTRA_REASON"
    }

    private lateinit var coordinator: AutomationCoordinator

    override fun onCreate() {
        super.onCreate()
        ABCNotificationManager.createChannels(this)
        startForeground(
            ABCNotificationManager.NOTIFICATION_ID_LOW,
            ABCNotificationManager.buildLowPriority(this)
        )
        val engine = AutomationEngine()
        val browser = AndroidBrowserController(this)
        coordinator = AutomationCoordinator(engine, browser)
        coordinator.onUserInteractionRequired = { jobId, reason, _ ->
            ABCWebViewHolder.setNeedsVerification(true)
            ABCNotificationManager.showHigh(this, reason, jobId)
        }
        coordinator.onResult = { jobId, value, type ->
            sendBroadcast(Intent(ACTION_LINK_EXTRACTED).apply {
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_RESULT_URL, value)
                putExtra(EXTRA_EVENT, type)
            })
        }
        coordinator.onTerminal = { jobId, status ->
            ABCNotificationManager.cancelHigh(this)
            ABCWebViewHolder.setNeedsVerification(false)
            sendBroadcast(Intent(ACTION_JOB_EVENT).apply {
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_EVENT, status)
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_JOB -> {
                coordinator.cancel()
                stopSelf()
            }
            ACTION_START_JOB, null -> {
                val url = intent?.getStringExtra(EXTRA_TARGET_URL)
                if (url.isNullOrBlank()) {
                    Timber.w("Missing target URL")
                    return START_NOT_STICKY
                }
                val script = intent.getStringExtra(EXTRA_SCRIPT)
                val job = AutomationJob(targetUrl = url, script = script)
                val id = coordinator.start(job)
                if (id == null) {
                    Timber.w("Job rejected")
                    sendBroadcast(Intent(ACTION_JOB_EVENT).apply {
                        putExtra(EXTRA_JOB_ID, job.id)
                        putExtra(EXTRA_EVENT, "JOB_REJECTED")
                    })
                }
            }
        }
        return START_NOT_STICKY
    }

    fun resumeAfterUserInteraction() {
        coordinator.resume()
        ABCNotificationManager.cancelHigh(this)
        ABCWebViewHolder.setNeedsVerification(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        coordinator.destroy()
        super.onDestroy()
    }
}
