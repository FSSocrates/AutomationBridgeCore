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
        assertEquals(AutomationState.RUNNING, engine.state.value)
    }

    @Test fun rejectWhileBusy() {
        engine.submit(AutomationJob(targetUrl = "https://example.com"))
        val second = engine.submit(AutomationJob(targetUrl = "https://other.com"))
        assertNull(second)
    }

    @Test fun userInteractionState() {
        engine.submit(AutomationJob(targetUrl = "https://example.com"))
        engine.requestUserInteraction("captcha")
        assertEquals(AutomationState.WAITING_FOR_USER, engine.state.value)
        engine.resumeAfterUserInteraction()
        assertEquals(AutomationState.RUNNING, engine.state.value)
    }

    @Test fun rejectBadUrl() {
        assertNull(engine.submit(AutomationJob(targetUrl = "javascript:alert(1)")))
    }
}
