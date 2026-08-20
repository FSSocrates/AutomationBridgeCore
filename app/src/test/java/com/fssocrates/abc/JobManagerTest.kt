package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.AutomationJobManager
import com.fssocrates.abc.core.SubmitResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class JobManagerTest {
    @Test fun submitAccepted() = runBlocking {
        val eng = AutomationEngine()
        val mgr = AutomationJobManager(eng)
        val r = mgr.submit(AutomationJob(targetUrl = "https://example.com"))
        assertTrue(r is SubmitResult.Accepted)
    }

    @Test fun rejectBadUrl() = runBlocking {
        val mgr = AutomationJobManager(AutomationEngine())
        val r = mgr.submit(AutomationJob(targetUrl = "javascript:x"))
        assertTrue(r is SubmitResult.Rejected)
    }

    @Test fun queueThenPoll() = runBlocking {
        val eng = AutomationEngine()
        val mgr = AutomationJobManager(eng)
        mgr.submit(AutomationJob(id = "A", targetUrl = "https://a.com"))
        mgr.submit(AutomationJob(id = "B", targetUrl = "https://b.com"))
        val first = mgr.pollNext()
        assertEquals("A", first?.id)
        // Engine not started so still canExecute — but active marked RUNNING in store
    }
}
