package com.fssocrates.abc.core

import java.util.UUID

enum class AutomationState {
    IDLE, RUNNING, WAITING_FOR_USER, COMPLETED, FAILED, CANCELLED
}

data class AutomationJob(
    val id: String = "JOB-${UUID.randomUUID().toString().take(8)}",
    val targetUrl: String,
    val script: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AutomationResult(
    val jobId: String,
    val type: String = "URL",
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
    data class UserInteractionRequired(override val jobId: String, val reason: String, val message: String? = null) : AutomationEvent
    data class Result(override val jobId: String, val value: String, val type: String = "URL") : AutomationEvent
    data class Failed(override val jobId: String, val error: String) : AutomationEvent
    data class Completed(override val jobId: String) : AutomationEvent
    data class Cancelled(override val jobId: String) : AutomationEvent
}
