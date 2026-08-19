package com.fssocrates.abc

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import timber.log.Timber

/** Thin controller around the shared WebView. */
class WebViewController(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())

    fun create(): WebView = ABCWebViewHolder.getOrCreate(context)

    fun load(url: String) {
        main.post {
            ABCWebViewHolder.getOrCreate(context).loadUrl(url)
        }
    }

    fun execute(script: String) {
        main.post {
            ABCWebViewHolder.get()?.evaluateJavascript(script, null)
                ?: Timber.w("No WebView for script")
        }
    }

    fun setClient(client: WebViewClient) {
        main.post { ABCWebViewHolder.getOrCreate(context).webViewClient = client }
    }

    fun attach() = ABCWebViewHolder.markAttachedToUi(true)
    fun detach() = ABCWebViewHolder.markAttachedToUi(false)

    fun stop() {
        main.post {
            ABCWebViewHolder.get()?.apply {
                stopLoading()
                loadUrl("about:blank")
            }
        }
    }

    fun destroy() = ABCWebViewHolder.destroy()
}
