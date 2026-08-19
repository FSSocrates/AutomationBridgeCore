package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.EngineState
import com.fssocrates.abc.core.JobState
import org.junit.Assert.*
import org.junit.Test

class EngineTest {
    private fun tick() = Thread.sleep(80)

    @Test fun startExecution() {
        val e = AutomationEngine()
        e.startExecution(AutomationJob(targetUrl = "https://example.com"))
        tick()
        assertEquals(EngineState.EXECUTING, e.engineState.value)
        assertEquals(JobState.RUNNING, e.jobState.value)
    }

    @Test fun resultDoesNotComplete() {
        val e = AutomationEngine()
        e.startExecution(AutomationJob(targetUrl = "https://example.com"))
        tick()
        e.produceResult("https://out")
        tick()
        assertEquals(JobState.RUNNING, e.jobState.value)
        assertEquals("https://out", e.lastResult?.value)
        e.complete()
        tick()
        assertEquals(EngineState.IDLE, e.engineState.value)
    }

    @Test fun rejectBadUrl() {
        val e = AutomationEngine()
        assertFalse(e.isUrlAllowed("javascript:alert(1)"))
    }
}
