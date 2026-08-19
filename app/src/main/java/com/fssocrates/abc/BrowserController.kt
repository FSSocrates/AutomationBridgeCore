package com.fssocrates.abc

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import timber.log.Timber

/** Small WebView abstraction. No job/state knowledge. */
interface BrowserController {
    fun load(url: String)
    fun executeScript(script: String)
    fun stop()
    fun destroy()
    fun setPageFinishedListener(listener: ((String) -> Unit)?)
    fun getWebView(): WebView?
}

class AndroidBrowserController(private val context: Context) : BrowserController {
    private val main = Handler(Looper.getMainLooper())
    private var pageFinishedListener: ((String) -> Unit)? = null

    init {
        main.post {
            val wv = ABCWebViewHolder.getOrCreate(context)
            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let { pageFinishedListener?.invoke(it) }
                }
            }
        }
    }

    override fun load(url: String) {
        main.post { ABCWebViewHolder.getOrCreate(context).loadUrl(url) }
    }

    override fun executeScript(script: String) {
        main.post {
            ABCWebViewHolder.get()?.evaluateJavascript(script, null)
                ?: Timber.w("No WebView for script")
        }
    }

    override fun stop() {
        main.post {
            ABCWebViewHolder.get()?.apply {
                stopLoading()
                loadUrl("about:blank")
            }
        }
    }

    override fun destroy() {
        ABCWebViewHolder.destroy()
    }

    override fun setPageFinishedListener(listener: ((String) -> Unit)?) {
        pageFinishedListener = listener
    }

    override fun getWebView(): WebView? = ABCWebViewHolder.get()
}
