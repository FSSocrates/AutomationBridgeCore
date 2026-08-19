package com.fssocrates.abc

import com.fssocrates.abc.core.AutomationJob
import com.fssocrates.abc.core.JobQueue
import org.junit.Assert.*
import org.junit.Test

class JobQueueTest {
    @Test fun fifo() {
        val q = JobQueue()
        q.enqueue(AutomationJob(id = "A", targetUrl = "https://a.com"))
        q.enqueue(AutomationJob(id = "B", targetUrl = "https://b.com"))
        assertEquals("A", q.poll()?.id)
        assertEquals("B", q.poll()?.id)
        assertNull(q.poll())
    }
}
