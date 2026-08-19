package com.fssocrates.abc.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single-job automation engine. Owns state machine and current job.
 * Android Service / WebView are adapters around this.
 */
class AutomationEngine(
    private val urlPolicy: UrlPolicy = UrlPolicy(),
    private val scriptPolicy: ScriptPolicy = ScriptPolicy()
) {
    private val _state = MutableStateFlow(AutomationState.IDLE)
    val state: StateFlow<AutomationState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AutomationEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<AutomationEvent> = _events.asSharedFlow()

    @Volatile
    var currentJob: AutomationJob? = null
        private set

    fun canSubmit(): Boolean = _state.value == AutomationState.IDLE

    /** Returns job id or null if rejected (busy / invalid). */
    fun submit(job: AutomationJob): String? {
        if (!canSubmit()) return null
        if (!isUrlAllowed(job.targetUrl)) return null
        if (job.script != null && !isScriptAllowed(job.script)) return null

        currentJob = job
        _state.value = AutomationState.RUNNING
        _events.tryEmit(AutomationEvent.Started(job.id))
        return job.id
    }

    fun onPageLoaded(url: String) {
        val job = currentJob ?: return
        _events.tryEmit(AutomationEvent.PageLoaded(job.id, url))
    }

    fun requestUserInteraction(reason: String, message: String? = null) {
        val job = currentJob ?: return
        if (_state.value != AutomationState.RUNNING) return
        _state.value = AutomationState.WAITING_FOR_USER
        _events.tryEmit(AutomationEvent.UserInteractionRequired(job.id, reason, message))
    }

    fun resumeAfterUserInteraction() {
        val job = currentJob ?: return
        if (_state.value != AutomationState.WAITING_FOR_USER) return
        _state.value = AutomationState.RUNNING
    }

    fun deliverResult(value: String, type: String = "URL") {
        val job = currentJob ?: return
        _events.tryEmit(AutomationEvent.Result(job.id, value, type))
        complete()
    }

    fun fail(error: String) {
        val job = currentJob ?: return
        _state.value = AutomationState.FAILED
        _events.tryEmit(AutomationEvent.Failed(job.id, error))
        clear()
    }

    fun cancel() {
        val job = currentJob ?: return
        _state.value = AutomationState.CANCELLED
        _events.tryEmit(AutomationEvent.Cancelled(job.id))
        clear()
    }

    private fun complete() {
        val job = currentJob ?: return
        _state.value = AutomationState.COMPLETED
        _events.tryEmit(AutomationEvent.Completed(job.id))
        clear()
    }

    private fun clear() {
        currentJob = null
        _state.value = AutomationState.IDLE
    }

    fun isUrlAllowed(url: String): Boolean = try {
        val uri = java.net.URI(url.trim())
        val scheme = uri.scheme?.lowercase() ?: return false
        scheme in urlPolicy.allowedSchemes &&
            (urlPolicy.allowHttp || scheme != "http") &&
            !uri.host.isNullOrBlank()
    } catch (_: Exception) { false }

    fun isScriptAllowed(script: String): Boolean {
        if (script.length > scriptPolicy.maxLength) return false
        val lower = script.lowercase()
        if (!scriptPolicy.allowNetworkApis &&
            (lower.contains("fetch(") || lower.contains("xmlhttprequest"))) return false
        if (!scriptPolicy.allowStorage &&
            (lower.contains("localstorage") || lower.contains("sessionstorage"))) return false
        return true
    }
}
