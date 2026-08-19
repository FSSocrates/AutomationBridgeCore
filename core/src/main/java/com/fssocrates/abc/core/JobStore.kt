package com.fssocrates.abc.core

/** In-memory job snapshot. Not durable across process death. */
class JobStore {
    @Volatile
    var active: AutomationJob? = null
        private set

    @Volatile
    var lastState: AutomationState = AutomationState.IDLE
        private set

    @Volatile
    var lastError: String? = null
        private set

    fun setActive(job: AutomationJob?) {
        active = job
    }

    fun setState(state: AutomationState) {
        lastState = state
    }

    fun setError(error: String?) {
        lastError = error
    }

    fun clear() {
        active = null
        lastState = AutomationState.IDLE
        lastError = null
    }
}
