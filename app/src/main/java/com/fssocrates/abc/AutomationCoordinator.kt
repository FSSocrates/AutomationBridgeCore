package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationEvent
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.AutomationOptions
import com.fssocrates.abc.core.AutomationState
import com.fssocrates.abc.core.JobStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class AutomationCoordinator(
    private val engine: AutomationEngine,
    private val browser: BrowserController,
    private val options: AutomationOptions = AutomationOptions(),
    private val jobStore: JobStore = JobStore()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingScript: String? = null
    private var timeoutJob: Job? = null

    var onUserInteractionRequired: ((jobId: String, reason: String, message: String?) -> Unit)? = null
    var onResult: ((jobId: String, value: String?, type: String) -> Unit)? = null
    var onTerminal: ((jobId: String, status: String, errorCode: String?, errorMessage: String?) -> Unit)? = null

    init {
        browser.setPageFinishedListener { url ->
            engine.onPageLoaded(url)
            pendingScript?.let { script ->
                if (engine.isScriptAllowed(script)) browser.executeScript(script)
                pendingScript = null
            }
        }
        scope.launch {
            engine.events.collectLatest { event ->
                when (event) {
                    is AutomationEvent.Started -> {
                        jobStore.setActive(engine.currentJob)
                        jobStore.setState(AutomationState.RUNNING)
                        Timber.i("ABC [%s] event=STARTED", event.jobId)
                        armOverallTimeout(event.jobId)
                    }
                    is AutomationEvent.PageLoaded ->
                        Timber.i("ABC [%s] event=PAGE_LOADED url=%s", event.jobId, event.url)
                    is AutomationEvent.UserInteractionRequired -> {
                        jobStore.setState(AutomationState.WAITING_FOR_USER)
                        Timber.i("ABC [%s] event=USER_INTERACTION reason=%s", event.jobId, event.reason)
                        armUserInteractionTimeout(event.jobId)
                        onUserInteractionRequired?.invoke(event.jobId, event.reason, event.message)
                    }
                    is AutomationEvent.Result -> {
                        Timber.i("ABC [%s] event=RESULT type=%s", event.jobId, event.result.type)
                        onResult?.invoke(event.jobId, event.result.value, event.result.type.name)
                    }
                    is AutomationEvent.Completed -> {
                        finishTerminal(event.jobId, "COMPLETED", null, null)
                    }
                    is AutomationEvent.Failed -> {
                        finishTerminal(event.jobId, "FAILED", "FAILED", event.error)
                    }
                    is AutomationEvent.Cancelled -> {
                        finishTerminal(event.jobId, "CANCELLED", "CALLER_CANCELLED", null)
                    }
                }
            }
        }
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
        timeoutJob?.cancel()
        engine.resumeAfterUserInteraction()
        pendingScript?.let { script ->
            if (engine.isScriptAllowed(script)) browser.executeScript(script)
            pendingScript = null
        }
    }

    fun cancel() {
        timeoutJob?.cancel()
        engine.cancel()
        browser.stop()
    }

    fun status(): Pair<String?, String> {
        val job = jobStore.active
        return job?.id to engine.state.value.name
    }

    private fun armOverallTimeout(jobId: String) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(options.overallTimeoutMs)
            if (engine.currentJob?.id == jobId) {
                engine.fail("TIMEOUT: overall")
            }
        }
    }

    private fun armUserInteractionTimeout(jobId: String) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(options.userInteractionTimeoutMs)
            if (engine.currentJob?.id == jobId &&
                engine.state.value == AutomationState.WAITING_FOR_USER
            ) {
                engine.fail("TIMEOUT: user_interaction")
            }
        }
    }

    private fun finishTerminal(
        jobId: String,
        status: String,
        errorCode: String?,
        errorMessage: String?
    ) {
        timeoutJob?.cancel()
        jobStore.setState(engine.state.value)
        jobStore.setError(errorMessage)
        Timber.i("ABC [%s] event=%s", jobId, status)
        onTerminal?.invoke(jobId, status, errorCode, errorMessage)
        jobStore.clear()
    }

    fun destroy() {
        timeoutJob?.cancel()
        browser.destroy()
        scope.cancel()
        ABC.engine = null
    }
}
