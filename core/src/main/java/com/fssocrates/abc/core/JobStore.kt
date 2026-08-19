package com.fssocrates.abc.core

class JobStore {
    @Volatile var active: AutomationJob? = null
        private set
    @Volatile var lastJobState: JobState? = null
        private set
    @Volatile var lastError: String? = null
        private set

    fun setActive(job: AutomationJob?) { active = job }
    fun setState(state: JobState?) { lastJobState = state }
    fun setError(error: String?) { lastError = error }
    fun clear() {
        active = null
        lastJobState = null
        lastError = null
    }
}
