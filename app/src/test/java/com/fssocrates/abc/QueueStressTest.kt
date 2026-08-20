package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.AutomationJobManager
import com.fssocrates.abc.core.AutomationOptions
import com.fssocrates.abc.core.SubmitResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class QueueStressTest {
    @Test fun manyQueuedFifo() = runBlocking {
        val eng = AutomationEngine()
        val mgr = AutomationJobManager(eng, options = AutomationOptions(maxQueueSize = 100))
        repeat(50) { i ->
            val r = mgr.submit(AutomationJob(id = "J$i", targetUrl = "https://example.com/$i"))
            assertTrue(r is SubmitResult.Accepted)
        }
        assertEquals(50, mgr.queueSize())
        assertEquals("J0", mgr.pollNext()?.id)
    }

    @Test fun queueFullRejected() = runBlocking {
        val eng = AutomationEngine()
        val mgr = AutomationJobManager(eng, options = AutomationOptions(maxQueueSize = 2))
        assertTrue(mgr.submit(AutomationJob(id = "1", targetUrl = "https://a.com")) is SubmitResult.Accepted)
        assertTrue(mgr.submit(AutomationJob(id = "2", targetUrl = "https://b.com")) is SubmitResult.Accepted)
        assertTrue(mgr.submit(AutomationJob(id = "3", targetUrl = "https://c.com")) is SubmitResult.Rejected)
    }
}
