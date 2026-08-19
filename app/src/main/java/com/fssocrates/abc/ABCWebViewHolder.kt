package com.fssocrates.abc

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe singleton holding the shared WebView.
 * Create/destroy always on main thread to avoid native leaks.
 */
object ABCWebViewHolder {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var webView: WebView? = null

    private val _isAttachedToUi = MutableStateFlow(false)
    val isAttachedToUi: StateFlow<Boolean> = _isAttachedToUi.asStateFlow()

    private val _needsVerification = MutableStateFlow(false)
    val needsVerification: StateFlow<Boolean> = _needsVerification.asStateFlow()

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreate(context: Context): WebView {
        webView?.let { return it }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalStateException("WebView must be created on main thread")
        }
        return synchronized(this) {
            webView ?: WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                addJavascriptInterface(ABC, "ABC")
                webViewClient = object : WebViewClient() {}
                webView = this
            }
        }
    }

    fun get(): WebView? = webView

    fun markAttachedToUi(attached: Boolean) {
        _isAttachedToUi.value = attached
    }

    fun setNeedsVerification(needs: Boolean) {
        _needsVerification.value = needs
    }

    fun destroy() {
        val toDestroy = synchronized(this) {
            val wv = webView
            webView = null
            _isAttachedToUi.value = false
            _needsVerification.value = false
            wv
        } ?: return

        val cleanup = {
            try {
                toDestroy.stopLoading()
                toDestroy.loadUrl("about:blank")
                toDestroy.removeAllViews()
                toDestroy.destroy()
            } catch (_: Exception) { }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            cleanup()
        } else {
            mainHandler.post(cleanup)
        }
    }
}
