// Example code for a caller app (not compiled in this project)
/*
val intent = Intent().apply {
    setClassName("com.fssocrates.abc", "com.fssocrates.abc.ABCForegroundService")
    putExtra("com.fssocrates.abc.EXTRA_TARGET_URL", "https://example.com")
    putExtra("com.fssocrates.abc.EXTRA_SCRIPT",
        "const a = document.querySelector('a'); if (a) ABC.sendResult(a.href); else ABC.triggerCaptcha();")
}
// Caller must declare <uses-permission android:name="com.fssocrates.abc.permission.START_ENGINE" />
context.startForegroundService(intent)
*/
