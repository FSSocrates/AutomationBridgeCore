package com.fssocrates.abc.core

import java.util.UUID

enum class AutomationState {
    IDLE, RUNNING, WAITING_FOR_USER, COMPLETED, FAILED, CANCELLED
}

enum class ResultType { URL, TEXT, JSON, EMPTY }

enum class UserInteractionReason {
    CAPTCHA, LOGIN, OTP, CONSENT, MANUAL_ACTION, OTHER
}

data class AutomationJob(
    val id: String = "JOB-${UUID.randomUUID().toString().take(8)}",
    val targetUrl: String,
    val script: String? = null,
    val createdAt: Long = System.currentTimeMillis()
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

sealed interface AutomationEvent {
    val jobId: String
    data class Started(override val jobId: String) : AutomationEvent
    data class PageLoaded(override val jobId: String, val url: String) : AutomationEvent
    data class UserInteractionRequired(
        override val jobId: String,
        val reason: String,
        val message: String? = null
    ) : AutomationEvent
    data class Result(override val jobId: String, val result: AutomationResult) : AutomationEvent
    data class Failed(override val jobId: String, val error: String) : AutomationEvent
    data class Completed(override val jobId: String) : AutomationEvent
    data class Cancelled(override val jobId: String) : AutomationEvent
}

/** Commands processed by the engine state machine. */
sealed interface EngineCommand {
    data class StartJob(val job: AutomationJob) : EngineCommand
    data object RequestUserInteraction : EngineCommand {
        var reason: String = "OTHER"
        var message: String? = null
        fun with(reason: String, message: String?) = apply {
            this.reason = reason
            this.message = message
        }
    }
    data object Resume : EngineCommand
    data class DeliverResult(val value: String, val type: ResultType = ResultType.URL) : EngineCommand
    data class Fail(val error: String) : EngineCommand
    data object Cancel : EngineCommand
    data object Reset : EngineCommand
}

data class AutomationOptions(
    val pageLoadTimeoutMs: Long = 30_000,
    val scriptTimeoutMs: Long = 60_000,
    val userInteractionTimeoutMs: Long = 300_000,
    val overallTimeoutMs: Long = 600_000
)
