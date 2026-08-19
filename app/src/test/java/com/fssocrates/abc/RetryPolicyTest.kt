package com.fssocrates.abc

import com.fssocrates.abc.core.RetryPolicy
import org.junit.Assert.*
import org.junit.Test

class RetryPolicyTest {
    @Test fun retriesTimeout() {
        val p = RetryPolicy(maxAttempts = 3)
        assertTrue(p.shouldRetry(1, "TIMEOUT"))
        assertFalse(p.shouldRetry(3, "TIMEOUT"))
        assertFalse(p.shouldRetry(1, "USER_CANCELLED"))
    }
}
