# JavaScript API v1

Injected as `ABC`.

| Method | Effect |
|--------|--------|
| `result(value)` / `result(value, type)` | Produce result; **does not** complete |
| `resultJson(json)` | Produce JSON result |
| `complete()` | Finish job successfully |
| `requestUserInteraction(reason, message?)` | Pause for human |
| `fail(message)` / `fail(code, message)` | Fail job |
| `log(message)` | Debug log |

Legacy: `sendResult` = result + complete; `triggerCaptcha` = requestUserInteraction("CAPTCHA").
