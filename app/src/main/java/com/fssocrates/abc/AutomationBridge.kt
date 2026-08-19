package com.fssocrates.abc

import android.content.Context
import android.content.Intent
import com.fssocrates.abc.core.AutomationJob

/** Public entry point for callers. Hides service implementation. */
object AutomationBridge {

    fun start(context: Context, url: String, script: String? = null) {
        val i = Intent(context, ABCForegroundService::class.java).apply {
            action = ABCForegroundService.ACTION_START_JOB
            putExtra(ABCForegroundService.EXTRA_TARGET_URL, url)
            putExtra(ABCForegroundService.EXTRA_SCRIPT, script)
        }
        context.startForegroundService(i)
    }

    fun start(context: Context, job: AutomationJob) {
        start(context, job.targetUrl, job.script)
    }

    fun cancel(context: Context, jobId: String? = null) {
        val i = Intent(context, ABCForegroundService::class.java).apply {
            action = ABCForegroundService.ACTION_CANCEL_JOB
            jobId?.let { putExtra(ABCForegroundService.EXTRA_JOB_ID, it) }
        }
        context.startService(i)
    }
}
