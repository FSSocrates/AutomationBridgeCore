# Workflow

```
submit → RUNNING → load → script
  ├─ result → COMPLETED
  ├─ fail → FAILED
  └─ requestUserInteraction → WAITING_FOR_USER
         └─ resume → RUNNING → …
```

Timeouts (defaults): page 30s, script 60s, user 5m, overall 10m.
