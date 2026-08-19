# IPC API (protocol v1)

## Actions
| Action | Purpose |
|--------|---------|
| `ACTION_START_JOB` | Submit job |
| `ACTION_CANCEL_JOB` | Cancel active job |
| `ACTION_STATUS` | Query status |

## Extras
`EXTRA_PROTOCOL_VERSION`, `EXTRA_JOB_ID`, `EXTRA_TARGET_URL`, `EXTRA_SCRIPT`,
`EXTRA_RESULT_URL`, `EXTRA_EVENT`, `EXTRA_STATUS`, `EXTRA_ERROR_CODE`, `EXTRA_ERROR_MESSAGE`, `EXTRA_REASON`

## Broadcasts
- `ACTION_LINK_EXTRACTED` — result
- `ACTION_JOB_EVENT` — status / terminal / rejected

## Permission
Caller must hold `com.fssocrates.abc.permission.USE_ENGINE` (signature).
