package com.fssocrates.abc.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns admission, queue, and persisted job records.
 * Does not touch WebView / Android UI.
 */
class AutomationJobManager(
    private val engine: AutomationEngine,
    private val queue: JobQueue = JobQueue(),
    private val store: JobStore = JobStore(),
    private val options: AutomationOptions = AutomationOptions(),
    private val urlPolicy: UrlPolicy = UrlPolicy(),
    private val scriptPolicy: ScriptPolicy = ScriptPolicy()
) {
    private val mutex = Mutex()
    private val records = LinkedHashMap<String, JobRecord>()

    data class JobRecord(
        val job: AutomationJob,
        var state: JobState,
        var phase: ExecutionPhase = ExecutionPhase.CREATED,
        var attempt: Int = 1,
        var maxAttempts: Int = 2,
        var result: AutomationResult? = null,
        var error: String? = null,
        val createdAt: Long = System.currentTimeMillis(),
        var updatedAt: Long = System.currentTimeMillis()
    )

    suspend fun submit(job: AutomationJob): SubmitResult = mutex.withLock {
        if (!isUrlAllowed(job.targetUrl)) {
            return SubmitResult.Rejected(RejectReason.INVALID_URL)
        }
        val script = job.script
        if (script != null && !isScriptAllowed(script)) {
            return SubmitResult.Rejected(RejectReason.SCRIPT_REJECTED)
        }
        if (records.containsKey(job.id)) {
            return SubmitResult.Rejected(RejectReason.DUPLICATE)
        }
        if (queue.size() >= options.maxQueueSize) {
            return SubmitResult.Rejected(RejectReason.QUEUE_FULL)
        }
        val record = JobRecord(job = job, state = JobState.QUEUED)
        records[job.id] = record
        store.setActive(null)
        queue.enqueue(job)
        return SubmitResult.Accepted(job.id)
    }

    /** Poll next job if engine idle. */
    suspend fun pollNext(): AutomationJob? = mutex.withLock {
        if (!engine.canExecute()) return null
        val job = queue.poll() ?: return null
        records[job.id]?.let {
            it.state = JobState.RUNNING
            it.phase = ExecutionPhase.LOADING
            it.updatedAt = System.currentTimeMillis()
        }
        store.setActive(job)
        store.setState(JobState.RUNNING)
        return job
    }

    suspend fun markWaiting(jobId: String) = mutex.withLock {
        records[jobId]?.apply {
            state = JobState.WAITING_FOR_USER
            phase = ExecutionPhase.WAITING_FOR_USER
            updatedAt = System.currentTimeMillis()
        }
        store.setState(JobState.WAITING_FOR_USER)
    }

    suspend fun markRunning(jobId: String) = mutex.withLock {
        records[jobId]?.apply {
            state = JobState.RUNNING
            phase = ExecutionPhase.EXECUTING
            updatedAt = System.currentTimeMillis()
        }
        store.setState(JobState.RUNNING)
    }

    suspend fun markResult(jobId: String, result: AutomationResult) = mutex.withLock {
        records[jobId]?.apply {
            this.result = result
            updatedAt = System.currentTimeMillis()
        }
    }

    suspend fun markTerminal(jobId: String, state: JobState, error: String? = null) =
        mutex.withLock {
            records[jobId]?.apply {
                this.state = state
                this.error = error
                phase = ExecutionPhase.COMPLETED
                updatedAt = System.currentTimeMillis()
            }
            store.clear()
        }

    suspend fun cancel(jobId: String): Boolean = mutex.withLock {
        val record = records[jobId] ?: return false
        when (record.state) {
            JobState.QUEUED -> {
                queue.remove(jobId)
                record.state = JobState.CANCELLED
                record.updatedAt = System.currentTimeMillis()
                true
            }
            JobState.RUNNING, JobState.WAITING_FOR_USER -> {
                // Caller must also stop engine/browser
                record.state = JobState.CANCELLED
                record.updatedAt = System.currentTimeMillis()
                store.clear()
                true
            }
            else -> false
        }
    }

    suspend fun status(jobId: String): JobRecord? = mutex.withLock { records[jobId] }

    suspend fun activeJobId(): String? = mutex.withLock {
        records.values.firstOrNull {
            it.state == JobState.RUNNING || it.state == JobState.WAITING_FOR_USER
        }?.job?.id
    }

    fun queueSize(): Int = queue.size()

    private fun isUrlAllowed(url: String): Boolean = try {
        val uri = java.net.URI(url.trim())
        val scheme = uri.scheme?.lowercase() ?: return false
        scheme in urlPolicy.allowedSchemes &&
            (urlPolicy.allowHttp || scheme != "http") &&
            !uri.host.isNullOrBlank()
    } catch (_: Exception) {
        false
    }

    private fun isScriptAllowed(script: String): Boolean {
        if (script.length > scriptPolicy.maxLength) return false
        val lower = script.lowercase()
        if (!scriptPolicy.allowNetworkApis &&
            (lower.contains("fetch(") || lower.contains("xmlhttprequest"))
        ) return false
        if (!scriptPolicy.allowStorage &&
            (lower.contains("localstorage") || lower.contains("sessionstorage"))
        ) return false
        return true
    }
}
