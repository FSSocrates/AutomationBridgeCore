# JavaScript API v1 (frozen for 1.0)

Injected as `ABC`.

| Method | Effect |
|--------|--------|
| `result(value)` / `result(value, type)` | Produce result; does **not** complete |
| `resultJson(json)` | Produce JSON result |
| `complete()` | Finish job successfully |
| `requestUserInteraction(reason, message?)` | Pause for human |
| `fail(message)` / `fail(code, message)` | Fail job |
| `log(message)` | Debug log |

Legacy (kept): `sendResult` = result+complete; `triggerCaptcha` = requestUserInteraction("CAPTCHA").
