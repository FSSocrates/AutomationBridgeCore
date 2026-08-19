package com.fssocrates.abc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.concurrent.ConcurrentHashMap

/** Routes results to per-job PendingIntents when provided. */
object ResultRouter {
    private val targets = ConcurrentHashMap<String, PendingIntent>()

    fun register(jobId: String, pi: PendingIntent) {
        targets[jobId] = pi
    }

    fun unregister(jobId: String) {
        targets.remove(jobId)
    }

    fun deliver(context: Context, jobId: String, value: String?, type: String, status: String) {
        val pi = targets.remove(jobId) ?: return
        val data = Intent().apply {
            putExtra(IpcProtocol.EXTRA_JOB_ID, jobId)
            putExtra(IpcProtocol.EXTRA_RESULT_URL, value)
            putExtra(IpcProtocol.EXTRA_EVENT, type)
            putExtra(IpcProtocol.EXTRA_STATUS, status)
            putExtra(IpcProtocol.EXTRA_PROTOCOL_VERSION, IpcProtocol.VERSION)
        }
        try {
            pi.send(context, 0, data)
        } catch (e: PendingIntent.CanceledException) {
            timber.log.Timber.w(e, "Result PendingIntent cancelled for %s", jobId)
        }
    }
}
