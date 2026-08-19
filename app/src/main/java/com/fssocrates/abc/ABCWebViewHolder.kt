package com.fssocrates.abc

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe singleton holding the shared WebView instance.
 * Allows clean attach/detach between ForegroundService and SolverActivity.
 */
object ABCWebViewHolder {

    @Volatile
    private var webView: WebView? = null

    private val _isAttachedToUi = MutableStateFlow(false)
    val isAttachedToUi: StateFlow<Boolean> = _isAttachedToUi.asStateFlow()

    private val _needsVerification = MutableStateFlow(false)
    val needsVerification: StateFlow<Boolean> = _needsVerification.asStateFlow()

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreate(context: Context): WebView {
        return webView ?: synchronized(this) {
            webView ?: WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.userAgentString = settings.userAgentString
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
        synchronized(this) {
            webView?.destroy()
            webView = null
            _isAttachedToUi.value = false
            _needsVerification.value = false
        }
    }
}
