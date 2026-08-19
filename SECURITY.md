# Security

## Threat model
- Untrusted apps must not submit jobs (signature permission `USE_ENGINE`).
- Scripts run inside WebView of the ABC process; who may submit is the real boundary.
- ScriptPolicy is a convenience filter, not a sandbox.

## Permissions
- `com.fssocrates.abc.permission.USE_ENGINE` — protectionLevel=signature

## Reporting
Open a private security advisory on the GitHub repo.
