package com.fssocrates.abc.core

data class RetryPolicy(
    val maxAttempts: Int = 1,
    val backoffMs: Long = 1_000,
    val retryableCodes: Set<String> = setOf("BROWSER_LOAD_FAILED", "TIMEOUT")
) {
    fun shouldRetry(attempt: Int, errorCode: String): Boolean =
        attempt < maxAttempts && errorCode in retryableCodes
}
