# AutomationBridgeCore (ABC)

Small **Android-native automation runtime**: authorized apps submit web jobs to a single WebView session, optionally involve a human, and receive deterministic results via IPC.

Not an AI browser agent or Playwright clone.

## Flow

```
App → Intent/Binder → Queue → Coordinator → WebView → JS bridge
                              ↓
                    WAITING_FOR_USER → ManualInteractionActivity
                              ↓
                           result / complete
```

## Quick start (same-app)

```kotlin
AutomationBridge.start(context, "https://example.com",
  script = "ABC.result(location.href); ABC.complete();")
```

## JS API v1

See [docs/JAVASCRIPT_API.md](docs/JAVASCRIPT_API.md).  
`result()` does **not** finish the job — call `complete()`.

## IPC v1

`START` / `CANCEL` / `STATUS` / `RESUME` — see [docs/API.md](docs/API.md).  
Optional `EXTRA_RESULT_PENDING_INTENT` for request-specific results.

## Modules

| Module | Role |
|--------|------|
| `:core` | Engine, jobs, queue, policies |
| `:app` | Service, WebView, Compose UI, demo |

## License

Apache 2.0


## Publish core (local)

```bash
./gradlew :core:publishReleasePublicationToLocalBuildRepository
# artifacts under build/repo/
```
