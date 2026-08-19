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
 * Single-job automation engine. Android/WebView-free.
 * All mutations go through [transition] via a serialized command channel.
 */
class AutomationEngine(
    private val urlPolicy: UrlPolicy = UrlPolicy(),
    private val scriptPolicy: ScriptPolicy = ScriptPolicy()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val commands = Channel<EngineCommand>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(AutomationState.IDLE)
    val state: StateFlow<AutomationState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AutomationEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AutomationEvent> = _events.asSharedFlow()

    @Volatile
    var currentJob: AutomationJob? = null
        private set

    init {
        scope.launch {
            for (cmd in commands) {
                mutex.withLock { transition(cmd) }
            }
        }
    }

    fun submit(job: AutomationJob): String? {
        if (!isUrlAllowed(job.targetUrl)) return null
        if (job.script != null && !isScriptAllowed(job.script)) return null
        if (_state.value != AutomationState.IDLE) return null
        commands.trySend(EngineCommand.StartJob(job))
        return job.id
    }

    fun requestUserInteraction(reason: String, message: String? = null) {
        commands.trySend(
            EngineCommand.RequestUserInteraction.with(reason, message)
        )
    }

    fun resumeAfterUserInteraction() {
        commands.trySend(EngineCommand.Resume)
    }

    fun deliverResult(value: String, type: ResultType = ResultType.URL) {
        commands.trySend(EngineCommand.DeliverResult(value, type))
    }

    fun fail(error: String) {
        commands.trySend(EngineCommand.Fail(error))
    }

    fun cancel() {
        commands.trySend(EngineCommand.Cancel)
    }

    private fun transition(cmd: EngineCommand) {
        val from = _state.value
        when (cmd) {
            is EngineCommand.StartJob -> {
                if (from != AutomationState.IDLE) return
                currentJob = cmd.job
                _state.value = AutomationState.RUNNING
                _events.tryEmit(AutomationEvent.Started(cmd.job.id))
            }
            is EngineCommand.RequestUserInteraction -> {
                if (from != AutomationState.RUNNING) return
                val job = currentJob ?: return
                _state.value = AutomationState.WAITING_FOR_USER
                _events.tryEmit(
                    AutomationEvent.UserInteractionRequired(job.id, cmd.reason, cmd.message)
                )
            }
            is EngineCommand.Resume -> {
                if (from != AutomationState.WAITING_FOR_USER) return
                _state.value = AutomationState.RUNNING
            }
            is EngineCommand.DeliverResult -> {
                if (from != AutomationState.RUNNING) return
                val job = currentJob ?: return
                val result = AutomationResult(job.id, cmd.type, cmd.value)
                _events.tryEmit(AutomationEvent.Result(job.id, result))
                _state.value = AutomationState.COMPLETED
                _events.tryEmit(AutomationEvent.Completed(job.id))
                clear()
            }
            is EngineCommand.Fail -> {
                if (from != AutomationState.RUNNING && from != AutomationState.WAITING_FOR_USER) return
                val job = currentJob ?: return
                _state.value = AutomationState.FAILED
                _events.tryEmit(AutomationEvent.Failed(job.id, cmd.error))
                clear()
            }
            is EngineCommand.Cancel -> {
                if (from != AutomationState.RUNNING && from != AutomationState.WAITING_FOR_USER) return
                val job = currentJob ?: return
                _state.value = AutomationState.CANCELLED
                _events.tryEmit(AutomationEvent.Cancelled(job.id))
                clear()
            }
            is EngineCommand.Reset -> {
                if (from == AutomationState.COMPLETED ||
                    from == AutomationState.FAILED ||
                    from == AutomationState.CANCELLED ||
                    from == AutomationState.IDLE
                ) {
                    clear()
                }
            }
        }
    }

    private fun clear() {
        currentJob = null
        _state.value = AutomationState.IDLE
    }

    fun isUrlAllowed(url: String): Boolean = try {
        val uri = java.net.URI(url.trim())
        val scheme = uri.scheme?.lowercase() ?: return false
        scheme in urlPolicy.allowedSchemes &&
            (urlPolicy.allowHttp || scheme != "http") &&
            !uri.host.isNullOrBlank()
    } catch (_: Exception) {
        false
    }

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

    fun onPageLoaded(url: String) {
        val job = currentJob ?: return
        _events.tryEmit(AutomationEvent.PageLoaded(job.id, url))
    }
}
