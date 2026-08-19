package com.fssocrates.abc

import android.content.Context

/** Presents WAITING_FOR_USER via notification → ManualInteractionActivity. */
class UserInteractionController(private val context: Context) {
    fun request(jobId: String, reason: String, message: String?) {
        ABCWebViewHolder.setNeedsVerification(true)
        ABCNotificationManager.showHigh(context, reason, jobId)
    }

    fun dismiss() {
        ABCNotificationManager.cancelHigh(context)
        ABCWebViewHolder.setNeedsVerification(false)
    }
}
