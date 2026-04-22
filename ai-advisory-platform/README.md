# AI Advisory Platform

CLI-first AI advisory platform for SonarQube results.

## Run

```bash
python -m app.main --project-key payment-service --analysis-mode release_gate
```

## What it does

1. Fetches SonarQube issues for a project.
2. Normalizes and deduplicates findings.
3. Builds grouped risk context.
4. Routes the request to the proper AI advisory chain.
5. Returns structured JSON output.
