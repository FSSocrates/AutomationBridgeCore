# Security

## Threat model

**Assets:** Web session, cookies, page data, automation scripts, results.

**Threats:** Unauthorized caller, malicious page/JS, result interception, intent spoofing, WebView escape.

**Boundaries:**
- Caller auth: signature permission `USE_ENGINE`
- ScriptPolicy / UrlPolicy are convenience filters, not a sandbox
- Results: broadcast with jobId (prefer PendingIntent in future)

## Reporting

Private security advisory on the GitHub repo.
