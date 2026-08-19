package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationEngine
import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.JobState
import org.junit.Assert.*
import org.junit.Test

class TransitionTest {
    private fun tick() = Thread.sleep(80)
    private fun eng() = AutomationEngine()

    @Test fun runningToWaiting() {
        val e = eng()
        e.startExecution(AutomationJob(targetUrl = "https://a.com")); tick()
        e.requestUserInteraction("CAPTCHA"); tick()
        assertEquals(JobState.WAITING_FOR_USER, e.jobState.value)
    }

    @Test fun waitingToRunning() {
        val e = eng()
        e.startExecution(AutomationJob(targetUrl = "https://a.com")); tick()
        e.requestUserInteraction("OTP"); tick()
        e.resumeAfterUserInteraction(); tick()
        assertEquals(JobState.RUNNING, e.jobState.value)
    }

    @Test fun completeToIdle() {
        val e = eng()
        e.startExecution(AutomationJob(targetUrl = "https://a.com")); tick()
        e.complete(); tick()
        assertNull(e.currentJob)
    }
}
