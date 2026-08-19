package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationEvent
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.AutomationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Orchestrates Engine ↔ Browser. No Android UI/IPC knowledge.
 */
class AutomationCoordinator(
    private val engine: AutomationEngine,
    private val browser: BrowserController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingScript: String? = null

    var onUserInteractionRequired: ((jobId: String, reason: String, message: String?) -> Unit)? = null
    var onResult: ((jobId: String, value: String?, type: String) -> Unit)? = null
    var onTerminal: ((jobId: String, status: String) -> Unit)? = null

    init {
        browser.setPageFinishedListener { url ->
            engine.onPageLoaded(url)
            pendingScript?.let { script ->
                if (engine.isScriptAllowed(script)) {
                    browser.executeScript(script)
                }
                pendingScript = null
            }
        }
        scope.launch {
            engine.events.collectLatest { event ->
                when (event) {
                    is AutomationEvent.Started -> {
                        Timber.i("ABC [%s] event=STARTED", event.jobId)
                    }
                    is AutomationEvent.PageLoaded -> {
                        Timber.i("ABC [%s] event=PAGE_LOADED url=%s", event.jobId, event.url)
                    }
                    is AutomationEvent.UserInteractionRequired -> {
                        Timber.i("ABC [%s] event=USER_INTERACTION reason=%s", event.jobId, event.reason)
                        onUserInteractionRequired?.invoke(event.jobId, event.reason, event.message)
                    }
                    is AutomationEvent.Result -> {
                        Timber.i("ABC [%s] event=RESULT type=%s", event.jobId, event.result.type)
                        onResult?.invoke(
                            event.jobId,
                            event.result.value,
                            event.result.type.name
                        )
                    }
                    is AutomationEvent.Completed -> {
                        Timber.i("ABC [%s] event=COMPLETED", event.jobId)
                        onTerminal?.invoke(event.jobId, "COMPLETED")
                    }
                    is AutomationEvent.Failed -> {
                        Timber.w("ABC [%s] event=FAILED error=%s", event.jobId, event.error)
                        onTerminal?.invoke(event.jobId, "FAILED")
                    }
                    is AutomationEvent.Cancelled -> {
                        Timber.i("ABC [%s] event=CANCELLED", event.jobId)
                        onTerminal?.invoke(event.jobId, "CANCELLED")
                    }
                }
            }
        }
        // Wire JS bridge → engine via this coordinator
        ABC.engine = engine
    }

    fun start(job: AutomationJob): String? {
        val id = engine.submit(job) ?: return null
        pendingScript = job.script
        browser.load(job.targetUrl)
        return id
    }

    fun resume() {
        if (engine.state.value != AutomationState.WAITING_FOR_USER) return
        engine.resumeAfterUserInteraction()
        pendingScript?.let { script ->
            if (engine.isScriptAllowed(script)) browser.executeScript(script)
            pendingScript = null
        }
    }

    fun cancel() {
        engine.cancel()
        browser.stop()
    }

    fun destroy() {
        browser.destroy()
        scope.cancel()
        ABC.engine = null
    }
}
