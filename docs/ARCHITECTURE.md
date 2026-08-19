# Architecture

## Layers
- **JobManager path**: validate → persist → queue → worker
- **AutomationEngine**: engine/job/phase state only (no Android)
- **BrowserController**: disposable WebView per attempt
- **Coordinator**: admission + orchestration
- **IPC**: START / STATUS / CANCEL / RESUME

## States
- Engine: STOPPED | IDLE | EXECUTING
- Job: QUEUED | RUNNING | WAITING_FOR_USER | COMPLETED | FAILED | CANCELLED | INTERRUPTED
- Phase: CREATED → VALIDATING → LOADING → EXECUTING → WAITING_FOR_USER → FINALIZING → COMPLETED

## Invariants
- One active WebView execution; WAITING_FOR_USER holds the slot
- Retry = destroy WebView + new attempt
- `result` ≠ `complete`
