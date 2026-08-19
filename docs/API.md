# IPC API (protocol v1)

## Actions
| Action | Purpose |
|--------|---------|
| `ACTION_START_JOB` | Submit job (queued) |
| `ACTION_CANCEL_JOB` | Cancel + clear queue |
| `ACTION_STATUS` | Query status |
| `ACTION_RESUME` | Resume after user interaction |

## Extras
`EXTRA_PROTOCOL_VERSION`, `EXTRA_JOB_ID`, `EXTRA_TARGET_URL`, `EXTRA_SCRIPT`,
`EXTRA_RESULT_URL`, `EXTRA_EVENT`, `EXTRA_STATUS`, `EXTRA_ERROR_CODE`,
`EXTRA_ERROR_MESSAGE`, `EXTRA_REASON`, `EXTRA_RESULT_PENDING_INTENT`

## Broadcasts
- `ACTION_LINK_EXTRACTED` — result produced
- `ACTION_JOB_EVENT` — status / terminal / rejected

## Permission
`com.fssocrates.abc.permission.USE_ENGINE` (signature)
