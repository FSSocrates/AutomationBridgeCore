package com.fssocrates.abc.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Android-free state machine for the active job only.
 * Queue admission lives outside (JobManager / Coordinator).
 */
class AutomationEngine(
    private val urlPolicy: UrlPolicy = UrlPolicy(),
    private val scriptPolicy: ScriptPolicy = ScriptPolicy()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val commands = Channel<EngineCommand>(Channel.UNLIMITED)

    private val _engineState = MutableStateFlow(EngineState.IDLE)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _jobState = MutableStateFlow<JobState?>(null)
    val jobState: StateFlow<JobState?> = _jobState.asStateFlow()

    private val _phase = MutableStateFlow(ExecutionPhase.CREATED)
    val phase: StateFlow<ExecutionPhase> = _phase.asStateFlow()

    private val _events = MutableSharedFlow<AutomationEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AutomationEvent> = _events.asSharedFlow()

    @Volatile var currentJob: AutomationJob? = null
        private set

    /** Last result produced (result ≠ complete). */
    @Volatile var lastResult: AutomationResult? = null
        private set

    init {
        scope.launch {
            for (cmd in commands) mutex.withLock { transition(cmd) }
        }
    }

    fun isUrlAllowed(url: String): Boolean = try {
        val uri = java.net.URI(url.trim())
        val scheme = uri.scheme?.lowercase() ?: return false
        scheme in urlPolicy.allowedSchemes &&
            (urlPolicy.allowHttp || scheme != "http") &&
            !uri.host.isNullOrBlank()
    } catch (_: Exception) { false }

    fun isScriptAllowed(script: String): Boolean {
        if (script.length > scriptPolicy.maxLength) return false
        val lower = script.lowercase()
        if (!scriptPolicy.allowNetworkApis &&
            (lower.contains("fetch(") || lower.contains("xmlhttprequest"))
        ) return false
        if (!scriptPolicy.allowStorage &&
            (lower.contains("localstorage") || lower.contains("sessionstorage"))
        ) return false
        return true
    }

    fun canExecute(): Boolean =
        _engineState.value == EngineState.IDLE && currentJob == null

    fun startExecution(job: AutomationJob) {
        commands.trySend(EngineCommand.Admit(job))
    }

    fun requestUserInteraction(reason: String, message: String? = null) {
        commands.trySend(EngineCommand.RequestUserInteraction.with(reason, message))
    }

    fun resumeAfterUserInteraction() = commands.trySend(EngineCommand.Resume)

    /** Produce a result without completing the job. */
    fun produceResult(value: String, type: ResultType = ResultType.URL) {
        commands.trySend(EngineCommand.ProduceResult(value, type))
    }

    fun complete() = commands.trySend(EngineCommand.Complete)

    fun fail(error: String) = commands.trySend(EngineCommand.Fail(error))
    fun cancel() = commands.trySend(EngineCommand.Cancel)

    fun onPageLoaded(url: String) {
        val job = currentJob ?: return
        _phase.value = ExecutionPhase.EXECUTING
        _events.tryEmit(AutomationEvent.PageLoaded(job.id, url))
    }

    // Legacy aliases used by older bridge / tests
    fun deliverResult(value: String, type: ResultType = ResultType.URL) {
        produceResult(value, type)
        complete()
    }

    private fun transition(cmd: EngineCommand) {
        when (cmd) {
            is EngineCommand.Admit -> {
                if (!canExecute()) return
                currentJob = cmd.job
                lastResult = null
                _engineState.value = EngineState.EXECUTING
                _jobState.value = JobState.RUNNING
                _phase.value = ExecutionPhase.LOADING
                _events.tryEmit(AutomationEvent.Started(cmd.job.id, cmd.job.attempt))
            }
            is EngineCommand.RequestUserInteraction -> {
                if (_jobState.value != JobState.RUNNING) return
                val job = currentJob ?: return
                _jobState.value = JobState.WAITING_FOR_USER
                _phase.value = ExecutionPhase.WAITING_FOR_USER
                _events.tryEmit(
                    AutomationEvent.UserInteractionRequired(job.id, cmd.reason, cmd.message)
                )
            }
            is EngineCommand.Resume -> {
                if (_jobState.value != JobState.WAITING_FOR_USER) return
                _jobState.value = JobState.RUNNING
                _phase.value = ExecutionPhase.EXECUTING
            }
            is EngineCommand.ProduceResult -> {
                if (_jobState.value != JobState.RUNNING &&
                    _jobState.value != JobState.WAITING_FOR_USER
                ) return
                val job = currentJob ?: return
                val result = AutomationResult(job.id, cmd.type, cmd.value)
                lastResult = result
                _events.tryEmit(AutomationEvent.ResultProduced(job.id, result))
            }
            is EngineCommand.Complete -> {
                if (currentJob == null) return
                val job = currentJob ?: return
                _phase.value = ExecutionPhase.FINALIZING
                _jobState.value = JobState.COMPLETED
                _events.tryEmit(AutomationEvent.Completed(job.id))
                clearToIdle()
            }
            is EngineCommand.Fail -> {
                if (currentJob == null) return
                val job = currentJob ?: return
                _jobState.value = JobState.FAILED
                _events.tryEmit(AutomationEvent.Failed(job.id, cmd.error))
                clearToIdle()
            }
            is EngineCommand.Cancel -> {
                if (currentJob == null) return
                val job = currentJob ?: return
                _jobState.value = JobState.CANCELLED
                _events.tryEmit(AutomationEvent.Cancelled(job.id))
                clearToIdle()
            }
        }
    }

    private fun clearToIdle() {
        currentJob = null
        _engineState.value = EngineState.IDLE
        _phase.value = ExecutionPhase.COMPLETED
        // jobState left as terminal until next admit overwrites
    }
}
