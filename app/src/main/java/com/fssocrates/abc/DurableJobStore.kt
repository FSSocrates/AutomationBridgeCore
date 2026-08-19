package com.fssocrates.abc

import android.content.Context
import com.fssocrates.abc.core.JobState

/** Lightweight durable snapshot (not full WebView recovery). */
class DurableJobStore(context: Context) {
    private val prefs = context.getSharedPreferences("abc_job_store", Context.MODE_PRIVATE)

    fun save(jobId: String?, state: String, error: String? = null) {
        prefs.edit()
            .putString("jobId", jobId)
            .putString("state", state)
            .putString("error", error)
            .putLong("updatedAt", System.currentTimeMillis())
            .apply()
    }

    fun load(): Triple<String?, String, String?> {
        val id = prefs.getString("jobId", null)
        val state = prefs.getString("state", "IDLE") ?: "IDLE"
        val error = prefs.getString("error", null)
        return Triple(id, state, error)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
