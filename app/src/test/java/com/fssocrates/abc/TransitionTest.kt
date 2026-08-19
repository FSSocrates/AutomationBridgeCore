package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.AutomationState
import org.junit.Assert.*
import org.junit.Test

class TransitionTest {
    private fun eng() = AutomationEngine()
    private fun tick() = Thread.sleep(80)

    @Test fun idleToRunning() {
        val e = eng()
        e.submit(AutomationJob(targetUrl = "https://a.com"))
        tick()
        assertEquals(AutomationState.RUNNING, e.state.value)
    }

    @Test fun runningToWaiting() {
        val e = eng()
        e.submit(AutomationJob(targetUrl = "https://a.com")); tick()
        e.requestUserInteraction("CAPTCHA"); tick()
        assertEquals(AutomationState.WAITING_FOR_USER, e.state.value)
    }

    @Test fun waitingToRunning() {
        val e = eng()
        e.submit(AutomationJob(targetUrl = "https://a.com")); tick()
        e.requestUserInteraction("OTP"); tick()
        e.resumeAfterUserInteraction(); tick()
        assertEquals(AutomationState.RUNNING, e.state.value)
    }

    @Test fun runningToCompleted() {
        val e = eng()
        e.submit(AutomationJob(targetUrl = "https://a.com")); tick()
        e.deliverResult("https://out"); tick()
        assertEquals(AutomationState.IDLE, e.state.value)
    }

    @Test fun rejectResumeFromIdle() {
        val e = eng()
        e.resumeAfterUserInteraction(); tick()
        assertEquals(AutomationState.IDLE, e.state.value)
    }

    @Test fun cancelFromRunning() {
        val e = eng()
        e.submit(AutomationJob(targetUrl = "https://a.com")); tick()
        e.cancel(); tick()
        assertEquals(AutomationState.IDLE, e.state.value)
    }
}
