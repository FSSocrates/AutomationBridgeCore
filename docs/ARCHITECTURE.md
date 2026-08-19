# Architecture

## Layer contracts

| Layer | Knows | Must not know |
|-------|-------|----------------|
| AutomationEngine | jobs, states, events, policies | Context, WebView, Activity, Intents |
| BrowserController | WebView load/execute/lifecycle | job state, notifications, IPC |
| AutomationCoordinator | Engine ↔ Browser wiring | IPC details, UI widgets |
| ABCForegroundService | IPC, notifications, host lifecycle | state-machine rules |
| ManualInteractionActivity | present WebView for user | job validation |

## State transitions

| From | Command | To |
|------|---------|-----|
| IDLE | StartJob | RUNNING |
| RUNNING | RequestUserInteraction | WAITING_FOR_USER |
| WAITING_FOR_USER | Resume | RUNNING |
| RUNNING | DeliverResult | COMPLETED → IDLE |
| RUNNING / WAITING_FOR_USER | Fail | FAILED → IDLE |
| RUNNING / WAITING_FOR_USER | Cancel | CANCELLED → IDLE |

All other transitions are rejected.
