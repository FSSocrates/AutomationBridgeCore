package com.fssocrates.abc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.fssocrates.abc.core.AutomationJob

object AutomationBridge {
    const val PROTOCOL_VERSION = IpcProtocol.VERSION

    fun start(
        context: Context,
        url: String,
        script: String? = null,
        resultPendingIntent: PendingIntent? = null
    ) {
        context.startForegroundService(Intent(context, ABCForegroundService::class.java).apply {
            action = IpcProtocol.ACTION_START
            putExtra(IpcProtocol.EXTRA_PROTOCOL_VERSION, IpcProtocol.VERSION)
            putExtra(IpcProtocol.EXTRA_TARGET_URL, url)
            putExtra(IpcProtocol.EXTRA_SCRIPT, script)
            resultPendingIntent?.let {
                putExtra(IpcProtocol.EXTRA_RESULT_PENDING_INTENT, it)
            }
        })
    }

    fun start(context: Context, job: AutomationJob) = start(context, job.targetUrl, job.script)

    fun cancel(context: Context, jobId: String? = null) {
        context.startService(Intent(context, ABCForegroundService::class.java).apply {
            action = IpcProtocol.ACTION_CANCEL
            jobId?.let { putExtra(IpcProtocol.EXTRA_JOB_ID, it) }
        })
    }

    fun resume(context: Context) {
        context.startService(Intent(context, ABCForegroundService::class.java).apply {
            action = IpcProtocol.ACTION_RESUME
        })
    }

    fun status(context: Context) {
        context.startService(Intent(context, ABCForegroundService::class.java).apply {
            action = IpcProtocol.ACTION_STATUS
        })
    }
}
