# AutomationBridgeCore (ABC)

**Android automation engine** that lets authorized apps submit web-automation jobs to a background WebView, run controlled JavaScript workflows, request user interaction when needed, and receive structured results.

> CAPTCHA / Cloudflare / login / OTP are just reasons for `WAITING_FOR_USER` — not the core concept.

## Architecture (v0.2)

```
AutomationEngine (core)
       │
  AutomationJob + State Machine
       │
 ┌─────┼─────┐
 ▼     ▼     ▼
WebView  UserInteraction  Result/IPC
```

- **Single-job**: one active job at a time; concurrent submits are rejected.
- **core/** module: pure engine, jobs, events, policies (unit-testable).
- **app/**: Android host (Service, WebView, Compose Solver UI) + demo.

## States

`IDLE → RUNNING → WAITING_FOR_USER → RUNNING → COMPLETED | FAILED | CANCELLED`

## JS Bridge (`ABC`)

```js
ABC.result(url)
ABC.resultJson(JSON.stringify({type:"download", value: url}))
ABC.requestUserInteraction("captcha")   // or "login", "otp", "consent", ...
ABC.log("debug")
ABC.fail("reason")
ABC.complete()
```

Legacy: `ABC.sendResult` / `ABC.triggerCaptcha` still work.

## IPC (caller)

```kotlin
val i = Intent(ABCForegroundService.ACTION_START_JOB).apply {
  setClassName("com.fssocrates.abc", "com.fssocrates.abc.ABCForegroundService")
  putExtra(ABCForegroundService.EXTRA_TARGET_URL, "https://example.com")
  putExtra(ABCForegroundService.EXTRA_SCRIPT, "/* script */")
}
// Requires signature permission com.fssocrates.abc.permission.USE_ENGINE
startForegroundService(i)
```

Listen: `ACTION_LINK_EXTRACTED` / `ACTION_JOB_EVENT` with `EXTRA_JOB_ID`.

## Modules

| Module | Role |
|--------|------|
| `:core` | AutomationEngine, Job, State, Events, Policies |
| `:app`  | Service, WebView holder, Notifications, Solver UI, demo |

## License

Apache 2.0
