package com.fssocrates.abc

import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * JS bridge injected as "ABC".
 * Protocol: result / requestUserInteraction / log / fail / complete
 */
object ABC {

    @Volatile
    var engine: com.fssocrates.abc.core.AutomationEngine? = null

    @JavascriptInterface
    fun result(value: String) {
        engine?.deliverResult(value, "URL")
    }

    @JavascriptInterface
    fun resultJson(json: String) {
        try {
            val obj = JSONObject(json)
            val type = obj.optString("type", "JSON")
            val value = obj.optString("value", json)
            engine?.deliverResult(value, type)
        } catch (_: Exception) {
            engine?.deliverResult(json, "JSON")
        }
    }

    @JavascriptInterface
    fun requestUserInteraction(reason: String) {
        engine?.requestUserInteraction(reason, null)
    }

    @JavascriptInterface
    fun requestUserInteraction(reason: String, message: String) {
        engine?.requestUserInteraction(reason, message)
    }

    @JavascriptInterface
    fun log(message: String) {
        timber.log.Timber.d("JS: %s", message)
    }

    @JavascriptInterface
    fun fail(message: String) {
        engine?.fail(message)
    }

    @JavascriptInterface
    fun complete() {
        // explicit complete without payload
        engine?.deliverResult("", "EMPTY")
    }

    // Backward-compat aliases
    @JavascriptInterface
    fun sendResult(url: String) = result(url)

    @JavascriptInterface
    fun triggerCaptcha() = requestUserInteraction("captcha")
}
