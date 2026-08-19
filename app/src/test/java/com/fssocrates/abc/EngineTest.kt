package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.AutomationState
import org.junit.Assert.*
import org.junit.Test

class EngineTest {
    private val engine = AutomationEngine()

    @Test fun submitTransitionsToRunning() {
        val id = engine.submit(AutomationJob(targetUrl = "https://example.com"))
        assertNotNull(id)
        // async channel — give a tick
        Thread.sleep(50)
        assertEquals(AutomationState.RUNNING, engine.state.value)
    }

    @Test fun rejectWhileBusy() {
        engine.submit(AutomationJob(targetUrl = "https://example.com"))
        Thread.sleep(50)
        val second = engine.submit(AutomationJob(targetUrl = "https://other.com"))
        assertNull(second)
    }

    @Test fun userInteractionState() {
        engine.submit(AutomationJob(targetUrl = "https://example.com"))
        Thread.sleep(50)
        engine.requestUserInteraction("captcha")
        Thread.sleep(50)
        assertEquals(AutomationState.WAITING_FOR_USER, engine.state.value)
        engine.resumeAfterUserInteraction()
        Thread.sleep(50)
        assertEquals(AutomationState.RUNNING, engine.state.value)
    }

    @Test fun rejectBadUrl() {
        assertNull(engine.submit(AutomationJob(targetUrl = "javascript:alert(1)")))
    }
}
