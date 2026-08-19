package com.fssocrates.abc

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URI

class ABCForegroundService : Service() {

    companion object {
        const val EXTRA_TARGET_URL = "com.fssocrates.abc.EXTRA_TARGET_URL"
        const val EXTRA_SCRIPT = "com.fssocrates.abc.EXTRA_SCRIPT"
        const val ACTION_LINK_EXTRACTED = "com.fssocrates.abc.ACTION_LINK_EXTRACTED"
        const val EXTRA_RESULT_URL = "com.fssocrates.abc.EXTRA_RESULT_URL"
        private const val TAG = "ABCForegroundService"
        private val ALLOWED_SCHEMES = setOf("https", "http")
    }

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
        ABC.resultCallback = { url -> broadcastResult(url) }
        ABC.captchaCallback = {
            ABCWebViewHolder.setNeedsVerification(true)
            ABCNotificationManager.showHigh(this)
        }
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pendingScript?.let { script ->
                    // Only evaluate if script passed basic sanitization
                    if (isScriptSafe(script)) {
                        view?.evaluateJavascript(script, null)
                    } else {
                        Log.w(TAG, "Rejected unsafe script")
                    }
                    pendingScript = null
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_STICKY
        val url = intent.getStringExtra(EXTRA_TARGET_URL)
        val script = intent.getStringExtra(EXTRA_SCRIPT)

        if (url.isNullOrBlank() || !isUrlSafe(url)) {
            Log.w(TAG, "Rejected invalid or unsafe URL: $url")
            return START_STICKY
        }
        if (script != null && !isScriptSafe(script)) {
            Log.w(TAG, "Rejected unsafe script")
            return START_STICKY
        }

        targetUrl = url
        pendingScript = script
        val wv = ABCWebViewHolder.getOrCreate(this)
        if (!ABCWebViewHolder.isAttachedToUi.value) {
            wv.loadUrl(url)
        }
        return START_STICKY
    }

    /** Basic URL validation – only http/https, no javascript: etc. */
    private fun isUrlSafe(url: String): Boolean {
        return try {
            val uri = URI(url.trim())
            uri.scheme?.lowercase() in ALLOWED_SCHEMES && !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Basic script sanitization.
     * Blocks obvious dangerous patterns. Prefer opcode-based commands in future.
     */
    private fun isScriptSafe(script: String): Boolean {
        val lower = script.lowercase()
        val blocked = listOf(
            "eval(", "function(", "settimeout", "setinterval",
            "xmlhttprequest", "fetch(", "import(", "require(",
            "document.write", "innerhtml", "localstorage",
            "sessionstorage", "indexeddb", "webkit", "chrome."
        )
        return blocked.none { lower.contains(it) } && script.length < 10_000
    }

    private fun broadcastResult(url: String) {
        if (!isUrlSafe(url)) return
        val result = Intent(ACTION_LINK_EXTRACTED).apply {
            putExtra(EXTRA_RESULT_URL, url)
        }
        sendBroadcast(result)
    }

    fun resumeAfterVerification() {
        ABCWebViewHolder.setNeedsVerification(false)
        ABCNotificationManager.cancelHigh(this)
        pendingScript?.let { script ->
            if (isScriptSafe(script)) {
                ABCWebViewHolder.get()?.evaluateJavascript(script, null)
            }
            pendingScript = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        ABCWebViewHolder.destroy()
        super.onDestroy()
    }
}
