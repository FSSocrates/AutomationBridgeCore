package com.fssocrates.abc

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ABCForegroundService : Service() {

    companion object {
        const val EXTRA_TARGET_URL = "com.fssocrates.abc.EXTRA_TARGET_URL"
        const val EXTRA_SCRIPT = "com.fssocrates.abc.EXTRA_SCRIPT"
        const val ACTION_LINK_EXTRACTED = "com.fssocrates.abc.ACTION_LINK_EXTRACTED"
        const val EXTRA_RESULT_URL = "com.fssocrates.abc.EXTRA_RESULT_URL"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pendingScript: String? = null
    private var targetUrl: String? = null

    override fun onCreate() {
        super.onCreate()
        ABCNotificationManager.createChannels(this)
        startForeground(
            ABCNotificationManager.NOTIFICATION_ID_LOW,
            ABCNotificationManager.buildLowPriority(this)
        )
        setupWebView()
    }

    private fun setupWebView() {
        val wv = ABCWebViewHolder.getOrCreate(this)
        ABC.resultCallback = { url ->
            broadcastResult(url)
        }
        ABC.captchaCallback = {
            ABCWebViewHolder.setNeedsVerification(true)
            ABCNotificationManager.showHigh(this)
        }
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pendingScript?.let { script ->
                    view?.evaluateJavascript(script, null)
                    pendingScript = null
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_STICKY
        targetUrl = intent.getStringExtra(EXTRA_TARGET_URL)
        pendingScript = intent.getStringExtra(EXTRA_SCRIPT)
        val url = targetUrl
        if (!url.isNullOrBlank()) {
            val wv = ABCWebViewHolder.getOrCreate(this)
            if (!ABCWebViewHolder.isAttachedToUi.value) {
                wv.loadUrl(url)
            }
        }
        return START_STICKY
    }

    private fun broadcastResult(url: String) {
        val result = Intent(ACTION_LINK_EXTRACTED).apply {
            putExtra(EXTRA_RESULT_URL, url)
            setPackage(packageName) // restrict if desired; remove for cross-app
        }
        sendBroadcast(result)
        // Optionally stop after result
        // stopSelf()
    }

    fun resumeAfterVerification() {
        ABCWebViewHolder.setNeedsVerification(false)
        ABCNotificationManager.showLow(this)
        // Re-evaluate any pending script if needed
        pendingScript?.let { script ->
            ABCWebViewHolder.get()?.evaluateJavascript(script, null)
            pendingScript = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        ABCWebViewHolder.destroy()
        super.onDestroy()
    }
}
