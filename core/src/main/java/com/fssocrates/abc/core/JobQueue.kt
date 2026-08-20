package com.fssocrates.abc.core

import java.util.ArrayDeque

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
    fun remove(jobId: String): Boolean {
        val it = queue.iterator()
        while (it.hasNext()) {
            if (it.next().id == jobId) {
                it.remove()
                return true
            }
        }
        return false
    }

    @Synchronized
    fun contains(jobId: String): Boolean = queue.any { it.id == jobId }

    @Synchronized
    fun size(): Int = queue.size

    @Synchronized
    fun clear() = queue.clear()
}
