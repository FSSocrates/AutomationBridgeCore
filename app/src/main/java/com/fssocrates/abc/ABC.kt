package com.fssocrates.abc

import android.webkit.JavascriptInterface
import com.fssocrates.abc.core.ResultType
import org.json.JSONObject
import timber.log.Timber

/**
 * JS API v1:
 * - result / resultJson → produce result (does NOT complete)
 * - complete() → finish job
 * - requestUserInteraction / fail / log
 */
object ABC {
    @Volatile
    var engine: com.fssocrates.abc.core.AutomationEngine? = null

    @JavascriptInterface
    fun result(value: String) {
        engine?.produceResult(value, ResultType.URL)
    }

    @JavascriptInterface
    fun result(value: String, type: String) {
        val t = runCatching { ResultType.valueOf(type.uppercase()) }.getOrDefault(ResultType.TEXT)
        engine?.produceResult(value, t)
    }

    @JavascriptInterface
    fun resultJson(json: String) {
        try {
            val obj = JSONObject(json)
            result(obj.optString("value", json), obj.optString("type", "JSON"))
        } catch (_: Exception) {
            engine?.produceResult(json, ResultType.JSON)
        }
    }

    @JavascriptInterface
    fun complete() {
        engine?.complete()
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
    fun fail(code: String, message: String) {
        engine?.fail("$code: $message")
    }

    // Legacy: result + complete
    @JavascriptInterface
    fun sendResult(url: String) {
        engine?.produceResult(url, ResultType.URL)
        engine?.complete()
    }

    @JavascriptInterface
    fun triggerCaptcha() = requestUserInteraction("CAPTCHA")
}
