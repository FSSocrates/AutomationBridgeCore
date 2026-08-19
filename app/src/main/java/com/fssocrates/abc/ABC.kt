package com.fssocrates.abc

import android.webkit.JavascriptInterface

/**
 * JavascriptInterface injected as "ABC".
 * Called from page scripts to communicate results / captcha events back to native.
 */
object ABC {

    @Volatile
    var resultCallback: ((String) -> Unit)? = null

    @Volatile
    var captchaCallback: (() -> Unit)? = null

    @JavascriptInterface
    fun sendResult(url: String) {
        resultCallback?.invoke(url)
    }

    @JavascriptInterface
    fun triggerCaptcha() {
        captchaCallback?.invoke()
    }
}
