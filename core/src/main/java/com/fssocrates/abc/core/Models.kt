package com.fssocrates.abc.core

import java.util.UUID

/** Process-level engine availability. */
enum class EngineState { STOPPED, IDLE, EXECUTING }

/** Lifecycle of a single job. */
enum class JobState {
    QUEUED, RUNNING, WAITING_FOR_USER, COMPLETED, FAILED, CANCELLED, INTERRUPTED
}

/** Fine-grained execution phase (diagnostics). */
enum class ExecutionPhase {
    CREATED, VALIDATING, LOADING, EXECUTING, WAITING_FOR_USER, FINALIZING, COMPLETED
}

enum class ResultType { URL, TEXT, JSON, EMPTY }

data class AutomationJob(
    val id: String = "JOB-${UUID.randomUUID().toString().take(8)}",
    val targetUrl: String,
    val script: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var attempt: Int = 1
)

data class AutomationResult(
    val jobId: String,
    val type: ResultType = ResultType.URL,
    val value: String? = null
)

data class ScriptPolicy(
    val maxLength: Int = 10_000,
    val allowNetworkApis: Boolean = false,
    val allowStorage: Boolean = false
)

data class UrlPolicy(
    val allowedSchemes: Set<String> = setOf("https", "http"),
    val allowHttp: Boolean = true
)

data class AutomationOptions(
    val pageLoadTimeoutMs: Long = 30_000,
    val scriptTimeoutMs: Long = 60_000,
    val userInteractionTimeoutMs: Long = 300_000,
    val overallTimeoutMs: Long = 600_000,
    val maxQueueSize: Int = 32
)

sealed interface SubmitResult {
    data class Accepted(val jobId: String) : SubmitResult
    data class Rejected(val reason: RejectReason) : SubmitResult
}

enum class RejectReason {
    INVALID_URL, SCRIPT_REJECTED, QUEUE_FULL, SHUTTING_DOWN, DUPLICATE
}

sealed interface AutomationEvent {
    val jobId: String
    data class Queued(override val jobId: String) : AutomationEvent
    data class Started(override val jobId: String, val attempt: Int) : AutomationEvent
    data class PageLoaded(override val jobId: String, val url: String) : AutomationEvent
    data class UserInteractionRequired(
        override val jobId: String,
        val reason: String,
        val message: String? = null
    ) : AutomationEvent
    data class ResultProduced(override val jobId: String, val result: AutomationResult) : AutomationEvent
    data class Completed(override val jobId: String) : AutomationEvent
    data class Failed(override val jobId: String, val error: String) : AutomationEvent
    data class Cancelled(override val jobId: String) : AutomationEvent
}

sealed interface EngineCommand {
    data class Admit(val job: AutomationJob) : EngineCommand
    data object RequestUserInteraction : EngineCommand {
        var reason: String = "OTHER"
        var message: String? = null
        fun with(r: String, m: String?) = apply { reason = r; message = m }
    }
    data object Resume : EngineCommand
    data class ProduceResult(val value: String, val type: ResultType = ResultType.URL) : EngineCommand
    data object Complete : EngineCommand
    data class Fail(val error: String) : EngineCommand
    data object Cancel : EngineCommand
}
