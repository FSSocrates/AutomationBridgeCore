package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationEvent
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.AutomationOptions
import com.fssocrates.abc.core.EngineState
import com.fssocrates.abc.core.JobQueue
import com.fssocrates.abc.core.JobStore
import com.fssocrates.abc.core.RejectReason
import com.fssocrates.abc.core.RetryPolicy
import com.fssocrates.abc.core.SubmitResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Authoritative admission: validate → enqueue → worker executes one at a time.
 * WAITING_FOR_USER holds the execution slot.
 * Retry = destroy WebView + fresh attempt.
 */
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
        scope.launch { engine.events.collectLatest { handleEvent(it) } }
        ABC.engine = engine
    }

    fun submit(job: AutomationJob): SubmitResult {
        if (!engine.isUrlAllowed(job.targetUrl)) {
            return SubmitResult.Rejected(RejectReason.INVALID_URL)
        }
        val script = job.script
        if (script != null && !engine.isScriptAllowed(script)) {
            return SubmitResult.Rejected(RejectReason.SCRIPT_REJECTED)
        }
        if (queue.size() >= options.maxQueueSize) {
            return SubmitResult.Rejected(RejectReason.QUEUE_FULL)
        }
        queue.enqueue(job)
        durableStore?.save(job.id, "QUEUED")
        Timber.i("ABC [%s] event=QUEUED size=%d", job.id, queue.size())
        pump()
        return SubmitResult.Accepted(job.id)
    }

    /** @deprecated use submit */
    fun start(job: AutomationJob): String? = when (val r = submit(job)) {
        is SubmitResult.Accepted -> r.jobId
        is SubmitResult.Rejected -> null
    }

    private fun pump() {
        if (engine.engineState.value != EngineState.IDLE) return
        val job = queue.poll() ?: return
        beginExecution(job)
    }

    private fun beginExecution(job: AutomationJob) {
        lastJob = job
        pendingScript = job.script
        // Fresh WebView environment per attempt
        browser.stop()
        durableStore?.save(job.id, "RUNNING")
        engine.startExecution(job)
        browser.load(job.targetUrl)
    }

    private fun handleEvent(event: AutomationEvent) {
        when (event) {
            is AutomationEvent.Started -> {
                Timber.i("ABC [%s] event=STARTED attempt=%d", event.jobId, event.attempt)
                armOverallTimeout(event.jobId)
            }
            is AutomationEvent.PageLoaded ->
                Timber.i("ABC [%s] event=PAGE_LOADED", event.jobId)
            is AutomationEvent.UserInteractionRequired -> {
                durableStore?.save(event.jobId, "WAITING_FOR_USER")
                armUserInteractionTimeout(event.jobId)
                onUserInteractionRequired?.invoke(event.jobId, event.reason, event.message)
            }
            is AutomationEvent.ResultProduced -> {
                Timber.i("ABC [%s] event=RESULT type=%s", event.jobId, event.result.type)
                onResult?.invoke(event.jobId, event.result.value, event.result.type.name)
            }
            is AutomationEvent.Completed -> finishTerminal(event.jobId, "COMPLETED", null, null)
            is AutomationEvent.Failed -> {
                val code = if (event.error.startsWith("TIMEOUT")) "TIMEOUT" else "FAILED"
                val job = lastJob
                if (job != null && retryPolicy.shouldRetry(job.attempt, code)) {
                    job.attempt++
                    Timber.w("ABC [%s] retry attempt=%d", event.jobId, job.attempt)
                    scope.launch {
                        delay(retryPolicy.backoffMs)
                        browser.destroy()
                        // recreate via load path
                        beginExecution(job)
                    }
                } else {
                    finishTerminal(event.jobId, "FAILED", code, event.error)
                }
            }
            is AutomationEvent.Cancelled ->
                finishTerminal(event.jobId, "CANCELLED", "CALLER_CANCELLED", null)
            is AutomationEvent.Queued -> {}
        }
    }

    fun resume() {
        timeoutJob?.cancel()
        engine.resumeAfterUserInteraction()
        pendingScript?.let {
            if (engine.isScriptAllowed(it)) browser.executeScript(it)
            pendingScript = null
        }
    }

    fun cancel() {
        timeoutJob?.cancel()
        queue.clear()
        engine.cancel()
        browser.stop()
    }

    fun status(): Pair<String?, String> {
        val id = engine.currentJob?.id ?: durableStore?.load()?.first
        val st = engine.jobState.value?.name ?: engine.engineState.value.name
        return id to st
    }

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
            if (engine.currentJob?.id == jobId) engine.fail("TIMEOUT: user_interaction")
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
        onTerminal?.invoke(jobId, status, errorCode, errorMessage)
        browser.stop()
        // WAITING_FOR_USER held the slot; now free → pump queue
        scope.launch {
            delay(50)
            pump()
        }
    }

    fun destroy() {
        timeoutJob?.cancel()
        browser.destroy()
        scope.cancel()
        ABC.engine = null
    }
}
