# Migration to 1.0

## Breaking vs prior 0.x
- Prefer `SubmitResult` / queue admission over fire-and-forget start
- JS: `result()` no longer completes; call `complete()`
- Use `IpcProtocol` constants; `ACTION_RESUME` for user-interaction resume
- Optional `EXTRA_RESULT_PENDING_INTENT` instead of global broadcast only

## Stable surface (1.0)
- `AutomationBridge.start|cancel|resume|status`
- `IpcProtocol` actions/extras/broadcasts
- JS: `result`, `resultJson`, `complete`, `requestUserInteraction`, `fail`, `log`
- Signature permission `USE_ENGINE`
