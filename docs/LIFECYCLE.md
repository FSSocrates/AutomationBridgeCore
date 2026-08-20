# Job lifecycle

## Ownership
| Component | Owns |
|-----------|------|
| AutomationJobManager | admission, queue, records |
| AutomationEngine | active job state machine |
| AutomationCoordinator | WebView execution workflow |
| BrowserController | WebView instance |

## Flow
```
submit → QUEUED → poll → RUNNING → LOADING → EXECUTING
  → (optional WAITING_FOR_USER → resume)
  → result* → complete → COMPLETED → next
```

WAITING_FOR_USER holds the single execution slot.
