package com.fssocrates.abc

import android.webkit.JavascriptInterface
import com.fssocrates.abc.core.ResultType
import org.json.JSONObject
import timber.log.Timber

/**
 * JS bridge protocol (injected as "ABC").
 * Does not own state — forwards to AutomationEngine via [engine].
 */
object ABC {

    @Volatile
    var engine: com.fssocrates.abc.core.AutomationEngine? = null

    @JavascriptInterface
    fun result(value: String) {
        engine?.deliverResult(value, ResultType.URL)
    }

    @JavascriptInterface
    fun result(value: String, type: String) {
        val t = runCatching { ResultType.valueOf(type.uppercase()) }.getOrDefault(ResultType.TEXT)
        engine?.deliverResult(value, t)
    }

    @JavascriptInterface
    fun resultJson(json: String) {
        try {
            val obj = JSONObject(json)
            val type = obj.optString("type", "JSON")
            val value = obj.optString("value", json)
            result(value, type)
        } catch (_: Exception) {
            engine?.deliverResult(json, ResultType.JSON)
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
        Timber.d("JS: %s", message)
    }

    @JavascriptInterface
    fun fail(message: String) {
        engine?.fail(message)
    }

    @JavascriptInterface
    fun complete() {
        engine?.deliverResult("", ResultType.EMPTY)
    }

    // Legacy
    @JavascriptInterface
    fun sendResult(url: String) = result(url)

    @JavascriptInterface
    fun triggerCaptcha() = requestUserInteraction("CAPTCHA")
}
