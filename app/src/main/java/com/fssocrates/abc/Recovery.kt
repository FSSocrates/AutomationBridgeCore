package com.fssocrates.abc

import timber.log.Timber

/**
 * On process start: RUNNING snapshots → INTERRUPTED (not silently resumed).
 * QUEUED can be re-offered by caller; we only mark interrupted.
 */
object Recovery {
    fun onServiceStart(store: DurableJobStore) {
        val (id, state, _) = store.load()
        if (id == null) return
        when (state) {
            "RUNNING", "EXECUTING", "LOADING" -> {
                Timber.w("ABC [%s] recovered as INTERRUPTED (was %s)", id, state)
                store.save(id, "INTERRUPTED", "SERVICE_RESTARTED")
            }
            "WAITING_FOR_USER" -> {
                // Keep snapshot; user may still complete via notification if job re-bound
                Timber.i("ABC [%s] was WAITING_FOR_USER before death", id)
            }
            else -> { /* terminal or idle */ }
        }
    }
}
