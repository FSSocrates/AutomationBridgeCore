package com.fssocrates.abc.core

sealed interface AutomationError {
    val code: String
    val message: String

    data class InvalidUrl(override val message: String = "Invalid or disallowed URL") : AutomationError {
        override val code = "INVALID_URL"
    }
    data class ScriptRejected(override val message: String = "Script rejected by policy") : AutomationError {
        override val code = "SCRIPT_REJECTED"
    }
    data class EngineBusy(override val message: String = "Engine busy") : AutomationError {
        override val code = "ENGINE_BUSY"
    }
    data class BrowserLoadFailed(override val message: String) : AutomationError {
        override val code = "BROWSER_LOAD_FAILED"
    }
    data class ScriptExecutionFailed(override val message: String) : AutomationError {
        override val code = "SCRIPT_EXECUTION_FAILED"
    }
    data class UserCancelled(override val message: String = "User cancelled") : AutomationError {
        override val code = "USER_CANCELLED"
    }
    data class CallerCancelled(override val message: String = "Caller cancelled") : AutomationError {
        override val code = "CALLER_CANCELLED"
    }
    data class Timeout(override val message: String) : AutomationError {
        override val code = "TIMEOUT"
    }
    data class ServiceStopped(override val message: String = "Service stopped") : AutomationError {
        override val code = "SERVICE_STOPPED"
    }
    data class InternalError(override val message: String) : AutomationError {
        override val code = "INTERNAL_ERROR"
    }
}
