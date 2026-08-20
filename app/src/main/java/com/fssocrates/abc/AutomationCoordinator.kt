package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationEvent
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.AutomationJobManager
import com.fssocrates.abc.core.AutomationOptions
import com.fssocrates.abc.core.JobState
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
 * Execution workflow only. Admission owned by [AutomationJobManager].
 */
class AutomationCoordinator(
    private val engine: AutomationEngine,
    private val browser: BrowserController,
    private val jobManager: AutomationJobManager,
    private val options: AutomationOptions = AutomationOptions(),
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
        var result: SubmitResult = SubmitResult.Rejected(
            com.fssocrates.abc.core.RejectReason.QUEUE_FULL
        )
        // Bridge suspend API for service callers
        kotlinx.coroutines.runBlocking {
            result = jobManager.submit(job)
            if (result is SubmitResult.Accepted) {
                durableStore?.save(job.id, "QUEUED")
                Timber.i("ABC [%s] event=QUEUED", job.id)
                pump()
            }
        }
        return result
    }

    private fun pump() {
        scope.launch {
            val job = jobManager.pollNext() ?: return@launch
            beginExecution(job)
        }
    }

    private fun beginExecution(job: AutomationJob) {
        lastJob = job
        pendingScript = job.script
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
                scope.launch { jobManager.markWaiting(event.jobId) }
                durableStore?.save(event.jobId, "WAITING_FOR_USER")
                armUserInteractionTimeout(event.jobId)
                onUserInteractionRequired?.invoke(event.jobId, event.reason, event.message)
            }
            is AutomationEvent.ResultProduced -> {
                scope.launch { jobManager.markResult(event.jobId, event.result) }
                onResult?.invoke(event.jobId, event.result.value, event.result.type.name)
            }
            is AutomationEvent.Completed -> {
                scope.launch { jobManager.markTerminal(event.jobId, JobState.COMPLETED) }
                finishTerminal(event.jobId, "COMPLETED", null, null)
            }
            is AutomationEvent.Failed -> {
                val code = if (event.error.startsWith("TIMEOUT")) "TIMEOUT" else "FAILED"
                val job = lastJob
                if (job != null && retryPolicy.shouldRetry(job.attempt, code)) {
                    job.attempt++
                    Timber.w("ABC [%s] retry attempt=%d", event.jobId, job.attempt)
                    scope.launch {
                        delay(retryPolicy.backoffMs)
                        browser.destroy()
                        beginExecution(job)
                    }
                } else {
                    scope.launch {
                        jobManager.markTerminal(event.jobId, JobState.FAILED, event.error)
                    }
                    finishTerminal(event.jobId, "FAILED", code, event.error)
                }
            }
            is AutomationEvent.Cancelled -> {
                scope.launch { jobManager.markTerminal(event.jobId, JobState.CANCELLED) }
                finishTerminal(event.jobId, "CANCELLED", "CALLER_CANCELLED", null)
            }
            is AutomationEvent.Queued -> {}
        }
    }

    fun resume() {
        timeoutJob?.cancel()
        val id = engine.currentJob?.id
        engine.resumeAfterUserInteraction()
        if (id != null) scope.launch { jobManager.markRunning(id) }
        pendingScript?.let {
            if (engine.isScriptAllowed(it)) browser.executeScript(it)
            pendingScript = null
        }
    }

    fun cancel(jobId: String? = null) {
        timeoutJob?.cancel()
        scope.launch {
            if (jobId != null) jobManager.cancel(jobId)
            else engine.currentJob?.id?.let { jobManager.cancel(it) }
        }
        engine.cancel()
        browser.stop()
        pump()
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
