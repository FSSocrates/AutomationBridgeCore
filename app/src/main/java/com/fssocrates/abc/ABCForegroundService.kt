package com.fssocrates.abc

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.SubmitResult
import timber.log.Timber

class ABCForegroundService : Service() {

    companion object {
        const val ACTION_START_JOB = IpcProtocol.ACTION_START
        const val ACTION_CANCEL_JOB = IpcProtocol.ACTION_CANCEL
        const val EXTRA_JOB_ID = IpcProtocol.EXTRA_JOB_ID
        const val EXTRA_TARGET_URL = IpcProtocol.EXTRA_TARGET_URL
        const val EXTRA_SCRIPT = IpcProtocol.EXTRA_SCRIPT
        const val ACTION_LINK_EXTRACTED = IpcProtocol.BROADCAST_RESULT
        const val ACTION_JOB_EVENT = IpcProtocol.BROADCAST_EVENT
        const val EXTRA_RESULT_URL = IpcProtocol.EXTRA_RESULT_URL
        const val EXTRA_EVENT = IpcProtocol.EXTRA_EVENT
        const val EXTRA_REASON = IpcProtocol.EXTRA_REASON
    }

    private lateinit var coordinator: AutomationCoordinator
    private lateinit var userInteraction: UserInteractionController
    private lateinit var durable: DurableJobStore

    override fun onCreate() {
        super.onCreate()
        durable = DurableJobStore(this)
        Recovery.onServiceStart(durable)
        ABCNotificationManager.createChannels(this)
        startForeground(
            ABCNotificationManager.NOTIFICATION_ID_LOW,
            ABCNotificationManager.buildLowPriority(this)
        )
        userInteraction = UserInteractionController(this)
        coordinator = AutomationCoordinator(AutomationEngine(), AndroidBrowserController(this))
        coordinator.durableStore = durable
        coordinator.onUserInteractionRequired = { jobId, reason, _ ->
            userInteraction.request(jobId, reason, null)
        }
        coordinator.onResult = { jobId, value, type ->
            ResultRouter.deliver(this, jobId, value, type, "RESULT")
            sendBroadcast(Intent(IpcProtocol.BROADCAST_RESULT).apply {
                putExtra(IpcProtocol.EXTRA_PROTOCOL_VERSION, IpcProtocol.VERSION)
                putExtra(IpcProtocol.EXTRA_JOB_ID, jobId)
                putExtra(IpcProtocol.EXTRA_RESULT_URL, value)
                putExtra(IpcProtocol.EXTRA_EVENT, type)
            })
        }
        coordinator.onTerminal = { jobId, status, errorCode, errorMessage ->
            userInteraction.dismiss()
            ResultRouter.deliver(this, jobId, null, status, status)
            ResultRouter.unregister(jobId)
            sendBroadcast(Intent(IpcProtocol.BROADCAST_EVENT).apply {
                putExtra(IpcProtocol.EXTRA_PROTOCOL_VERSION, IpcProtocol.VERSION)
                putExtra(IpcProtocol.EXTRA_JOB_ID, jobId)
                putExtra(IpcProtocol.EXTRA_EVENT, status)
                putExtra(IpcProtocol.EXTRA_STATUS, status)
                errorCode?.let { putExtra(IpcProtocol.EXTRA_ERROR_CODE, it) }
                errorMessage?.let { putExtra(IpcProtocol.EXTRA_ERROR_MESSAGE, it) }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IpcProtocol.ACTION_CANCEL, ACTION_CANCEL_JOB -> {
                coordinator.cancel()
                stopSelf()
            }
            IpcProtocol.ACTION_RESUME -> {
                coordinator.resume()
            }
            IpcProtocol.ACTION_STATUS -> {
                val (id, status) = coordinator.status()
                sendBroadcast(Intent(IpcProtocol.BROADCAST_EVENT).apply {
                    putExtra(IpcProtocol.EXTRA_PROTOCOL_VERSION, IpcProtocol.VERSION)
                    putExtra(IpcProtocol.EXTRA_JOB_ID, id)
                    putExtra(IpcProtocol.EXTRA_STATUS, status)
                    putExtra(IpcProtocol.EXTRA_EVENT, "STATUS")
                })
            }
            IpcProtocol.ACTION_START, ACTION_START_JOB, null -> {
                val url = intent?.getStringExtra(IpcProtocol.EXTRA_TARGET_URL)
                    ?: intent?.getStringExtra(EXTRA_TARGET_URL)
                if (url.isNullOrBlank()) {
                    Timber.w("Missing target URL")
                    return START_NOT_STICKY
                }
                val script = intent?.getStringExtra(IpcProtocol.EXTRA_SCRIPT)
                    ?: intent?.getStringExtra(EXTRA_SCRIPT)
                val job = AutomationJob(targetUrl = url, script = script)
                val pi = if (Build.VERSION.SDK_INT >= 33) {
                    intent?.getParcelableExtra(
                        IpcProtocol.EXTRA_RESULT_PENDING_INTENT,
                        PendingIntent::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(IpcProtocol.EXTRA_RESULT_PENDING_INTENT)
                }
                when (val result = coordinator.submit(job)) {
                    is SubmitResult.Accepted -> {
                        pi?.let { ResultRouter.register(result.jobId, it) }
                        Timber.i("Accepted %s", result.jobId)
                    }
                    is SubmitResult.Rejected -> {
                        sendBroadcast(Intent(IpcProtocol.BROADCAST_EVENT).apply {
                            putExtra(IpcProtocol.EXTRA_PROTOCOL_VERSION, IpcProtocol.VERSION)
                            putExtra(IpcProtocol.EXTRA_JOB_ID, job.id)
                            putExtra(IpcProtocol.EXTRA_EVENT, "JOB_REJECTED")
                            putExtra(IpcProtocol.EXTRA_ERROR_CODE, result.reason.name)
                        })
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        coordinator.destroy()
        super.onDestroy()
    }
}
