package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationEvent
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.AutomationOptions
import com.fssocrates.abc.core.AutomationState
import com.fssocrates.abc.core.JobQueue
import com.fssocrates.abc.core.JobStore
import com.fssocrates.abc.core.RetryPolicy
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
    private val jobStore: JobStore = JobStore(),
    private val queue: JobQueue = JobQueue(),
    private val retryPolicy: RetryPolicy = RetryPolicy(maxAttempts = 2)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingScript: String? = null
    private var timeoutJob: Job? = null
    private var attempt = 0
    private var lastJob: AutomationJob? = null

    var onUserInteractionRequired: ((jobId: String, reason: String, message: String?) -> Unit)? = null
    var onResult: ((jobId: String, value: String?, type: String) -> Unit)? = null
    var onTerminal: ((jobId: String, status: String, errorCode: String?, errorMessage: String?) -> Unit)? = null
    var durableStore: DurableJobStore? = null

    init {
        browser.setPageFinishedListener { url ->
            engine.onPageLoaded(url)
            pendingScript?.let { script ->
                if (engine.isScriptAllowed(script)) browser.executeScript(script)
                pendingScript = null
            }
        }
        scope.launch {
            engine.events.collectLatest { event -> handleEvent(event) }
        }
        ABC.engine = engine
    }

    private fun handleEvent(event: AutomationEvent) {
        when (event) {
            is AutomationEvent.Started -> {
                jobStore.setActive(engine.currentJob)
                jobStore.setState(AutomationState.RUNNING)
                durableStore?.save(event.jobId, "RUNNING")
                Timber.i("ABC [%s] event=STARTED attempt=%d", event.jobId, attempt)
                armOverallTimeout(event.jobId)
            }
            is AutomationEvent.PageLoaded ->
                Timber.i("ABC [%s] event=PAGE_LOADED url=%s", event.jobId, event.url)
            is AutomationEvent.UserInteractionRequired -> {
                jobStore.setState(AutomationState.WAITING_FOR_USER)
                durableStore?.save(event.jobId, "WAITING_FOR_USER")
                armUserInteractionTimeout(event.jobId)
                onUserInteractionRequired?.invoke(event.jobId, event.reason, event.message)
            }
            is AutomationEvent.Result ->
                onResult?.invoke(event.jobId, event.result.value, event.result.type.name)
            is AutomationEvent.Completed ->
                finishTerminal(event.jobId, "COMPLETED", null, null)
            is AutomationEvent.Failed -> {
                val code = if (event.error.startsWith("TIMEOUT")) "TIMEOUT" else "FAILED"
                if (retryPolicy.shouldRetry(attempt, code) && lastJob != null) {
                    attempt++
                    Timber.w("ABC [%s] retry attempt=%d", event.jobId, attempt)
                    val job = lastJob!!
                    scope.launch {
                        delay(retryPolicy.backoffMs)
                        pendingScript = job.script
                        engine.submit(job)
                        browser.load(job.targetUrl)
                    }
                } else {
                    finishTerminal(event.jobId, "FAILED", code, event.error)
                }
            }
            is AutomationEvent.Cancelled ->
                finishTerminal(event.jobId, "CANCELLED", "CALLER_CANCELLED", null)
        }
    }

    /** Enqueue or start immediately if idle. */
    fun start(job: AutomationJob): String? {
        if (engine.state.value != AutomationState.IDLE) {
            queue.enqueue(job)
            Timber.i("ABC queued job=%s size=%d", job.id, queue.size())
            return job.id
        }
        return startNow(job)
    }

    private fun startNow(job: AutomationJob): String? {
        attempt = 1
        lastJob = job
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
        queue.clear()
        engine.cancel()
        browser.stop()
    }

    fun status(): Pair<String?, String> =
        (jobStore.active?.id ?: durableStore?.load()?.first) to engine.state.value.name

    private fun armOverallTimeout(jobId: String) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(options.overallTimeoutMs)
            if (engine.currentJob?.id == jobId) engine.fail("TIMEOUT: overall")
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
        durableStore?.save(jobId, status, errorMessage)
        jobStore.clear()
        onTerminal?.invoke(jobId, status, errorCode, errorMessage)
        // Drain queue
        val next = queue.poll()
        if (next != null) {
            scope.launch {
                delay(100)
                startNow(next)
            }
        }
    }

    fun destroy() {
        timeoutJob?.cancel()
        browser.destroy()
        scope.cancel()
        ABC.engine = null
    }
}
