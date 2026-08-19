package com.fssocrates.abc

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationEvent
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.AutomationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val engine = AutomationEngine()
    private var pendingScript: String? = null

    override fun onCreate() {
        super.onCreate()
        ABC.engine = engine
        ABCNotificationManager.createChannels(this)
        startForeground(
            ABCNotificationManager.NOTIFICATION_ID_LOW,
            ABCNotificationManager.buildLowPriority(this)
        )
        setupWebView()
        observeEvents()
    }

    private fun observeEvents() {
        scope.launch {
            engine.events.collectLatest { event ->
                when (event) {
                    is AutomationEvent.UserInteractionRequired -> {
                        ABCWebViewHolder.setNeedsVerification(true)
                        ABCNotificationManager.showHigh(this@ABCForegroundService, event.reason, event.jobId)
                    }
                    is AutomationEvent.Result -> broadcastResult(event)
                    is AutomationEvent.Failed,
                    is AutomationEvent.Completed,
                    is AutomationEvent.Cancelled -> {
                        ABCNotificationManager.cancelHigh(this@ABCForegroundService)
                        ABCWebViewHolder.setNeedsVerification(false)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupWebView() {
        val wv = ABCWebViewHolder.getOrCreate(this)
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { engine.onPageLoaded(it) }
                pendingScript?.let { script ->
                    if (engine.isScriptAllowed(script)) {
                        view?.evaluateJavascript(script, null)
                    }
                    pendingScript = null
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_JOB -> {
                engine.cancel()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_JOB, null -> {
                val url = intent?.getStringExtra(EXTRA_TARGET_URL)
                val script = intent?.getStringExtra(EXTRA_SCRIPT)
                if (url.isNullOrBlank()) {
                    Timber.w("Missing target URL")
                    return START_NOT_STICKY
                }
                val job = AutomationJob(targetUrl = url, script = script)
                val accepted = engine.submit(job)
                if (accepted == null) {
                    Timber.w("Job rejected (busy or invalid)")
                    broadcastEvent("JOB_REJECTED", job.id)
                    return START_NOT_STICKY
                }
                pendingScript = script
                if (!ABCWebViewHolder.isAttachedToUi.value) {
                    ABCWebViewHolder.getOrCreate(this).loadUrl(url)
                }
            }
        }
        return START_NOT_STICKY
    }

    fun resumeAfterUserInteraction() {
        engine.resumeAfterUserInteraction()
        ABCNotificationManager.cancelHigh(this)
        ABCWebViewHolder.setNeedsVerification(false)
        // Re-run pending script if any
        pendingScript?.let { script ->
            if (engine.isScriptAllowed(script)) {
                ABCWebViewHolder.get()?.evaluateJavascript(script, null)
            }
            pendingScript = null
        }
    }

    private fun broadcastResult(event: AutomationEvent.Result) {
        sendBroadcast(Intent(ACTION_LINK_EXTRACTED).apply {
            putExtra(EXTRA_JOB_ID, event.jobId)
            putExtra(EXTRA_RESULT_URL, event.value)
            putExtra(EXTRA_EVENT, "RESULT")
        })
    }

    private fun broadcastEvent(type: String, jobId: String) {
        sendBroadcast(Intent(ACTION_JOB_EVENT).apply {
            putExtra(EXTRA_JOB_ID, jobId)
            putExtra(EXTRA_EVENT, type)
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine.cancel()
        ABC.engine = null
        ABCWebViewHolder.destroy()
        scope.cancel()
        super.onDestroy()
    }
}
