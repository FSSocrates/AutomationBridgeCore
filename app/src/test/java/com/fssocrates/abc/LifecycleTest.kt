package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.AutomationJobManager
import com.fssocrates.abc.core.JobState
import com.fssocrates.abc.core.SubmitResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LifecycleTest {
    private fun tick() = Thread.sleep(80)

    @Test fun queueThenRunComplete() = runBlocking {
        val eng = AutomationEngine()
        val mgr = AutomationJobManager(eng)
        assertTrue(mgr.submit(AutomationJob(id = "A", targetUrl = "https://a.com")) is SubmitResult.Accepted)
        val job = mgr.pollNext()
        assertEquals("A", job?.id)
        eng.startExecution(job!!)
        tick()
        eng.produceResult("https://out")
        tick()
        eng.complete()
        tick()
        assertNull(eng.currentJob)
    }

    @Test fun waitingHoldsSlot() = runBlocking {
        val eng = AutomationEngine()
        val mgr = AutomationJobManager(eng)
        mgr.submit(AutomationJob(id = "A", targetUrl = "https://a.com"))
        mgr.submit(AutomationJob(id = "B", targetUrl = "https://b.com"))
        eng.startExecution(mgr.pollNext()!!)
        tick()
        eng.requestUserInteraction("CAPTCHA")
        tick()
        assertEquals(JobState.WAITING_FOR_USER, eng.jobState.value)
        assertFalse(eng.canExecute())
        eng.resumeAfterUserInteraction()
        tick()
        eng.complete()
        tick()
        assertTrue(eng.canExecute())
        assertEquals("B", mgr.pollNext()?.id)
    }
}
