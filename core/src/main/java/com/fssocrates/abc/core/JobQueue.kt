package com.fssocrates.abc.core

import java.util.ArrayDeque

/** Sequential single-worker queue. Does not run jobs in parallel. */
class JobQueue {
    private val queue = ArrayDeque<AutomationJob>()

    @Synchronized
    fun enqueue(job: AutomationJob): Boolean {
        queue.addLast(job)
        return true
    }

    @Synchronized
    fun poll(): AutomationJob? = if (queue.isEmpty()) null else queue.removeFirst()

    @Synchronized
    fun peek(): AutomationJob? = queue.firstOrNull()

    @Synchronized
    fun size(): Int = queue.size

    @Synchronized
    fun clear() = queue.clear()
}
