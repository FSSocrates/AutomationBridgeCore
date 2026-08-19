# AutomationBridgeCore (ABC)

Open-source Android library / utility for processing web workflows via IPC Intents and an off-screen WebView running inside a Foreground Service.

**Package:** `com.fssocrates.abc`  
**Min SDK:** 26  
**UI:** 100% Jetpack Compose (Material3)  
**License:** Apache License 2.0

## Architecture

1. Caller apps send an Intent containing `EXTRA_TARGET_URL` + `EXTRA_SCRIPT`.
2. `ABCForegroundService` hosts a shared `WebView` (via `ABCWebViewHolder`).
3. On page load the supplied JS is evaluated. The page may call:
   - `ABC.sendResult(url)` → result is broadcast as `ACTION_LINK_EXTRACTED`
   - `ABC.triggerCaptcha()` → notification upgrades to high-priority heads-up
4. User taps the notification → `SolverActivity` mounts the live WebView with Compose `AndroidView`.
5. After verification the activity finishes, WebView returns to background, notification downgrades, engine continues.

## Setup

```kotlin
// settings.gradle.kts / build.gradle.kts already configured for Compose BOM + Material3
```

Clone and open in Android Studio. Grant notification permission on Android 13+.

## Caller App IPC Example

```kotlin
val intent = Intent().apply {
    setClassName(
        "com.fssocrates.abc",
        "com.fssocrates.abc.ABCForegroundService"
    )
    putExtra("com.fssocrates.abc.EXTRA_TARGET_URL", "https://target.example")
    putExtra(
        "com.fssocrates.abc.EXTRA_SCRIPT",
        """
        // your extraction logic
        const link = document.querySelector('a.download')?.href;
        if (link) ABC.sendResult(link);
        else ABC.triggerCaptcha();
        """.trimIndent()
    )
}
context.startForegroundService(intent)
```

Listen for results:

```kotlin
val filter = IntentFilter("com.fssocrates.abc.ACTION_LINK_EXTRACTED")
registerReceiver(object : BroadcastReceiver() {
    override fun onReceive(c: Context?, i: Intent?) {
        val url = i?.getStringExtra("com.fssocrates.abc.EXTRA_RESULT_URL")
        // handle url
    }
}, filter)
```

## Key Classes

| File | Role |
|------|------|
| `ABC.kt` | `@JavascriptInterface` bridge (`sendResult`, `triggerCaptcha`) |
| `ABCWebViewHolder.kt` | Thread-safe singleton WebView + state flows |
| `ABCForegroundService.kt` | Foreground engine, intent handling, JS eval |
| `ABCNotificationManager.kt` | Low / High priority notification channels |
| `SolverActivity.kt` / `SolverScreen.kt` | Compose UI that attaches the live WebView |

## Permissions

- `INTERNET`
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC`
- `POST_NOTIFICATIONS` (Android 13+)

## License

Apache License 2.0 – see [LICENSE](LICENSE).
