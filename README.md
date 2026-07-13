# Intelligent DevSecOps Automation Framework

An **AI-driven DevSecOps automation framework** for agent-based Jenkins pipelines. This repository implements the prototype described in the research paper:

**"An Intelligent DevSecOps Automation Framework for AI-Driven Security Analytics in Agent-Based Jenkins Pipelines"**

The framework integrates multiple security tools into a single CI/CD flow, normalizes heterogeneous findings, consolidates duplicated issues, prioritizes security risk, and generates an AI-assisted security advisory to support remediation and release decisions.

## Overview

Modern DevSecOps pipelines often produce fragmented outputs from different scanners. Ruff, Semgrep, SonarQube, and Trivy each report findings with different structures, severities, and scopes. This project addresses that gap by adding an orchestration and advisory layer on top of multi-tool scanning.

The framework is designed to:

- Execute security checks automatically inside an agent-based Jenkins pipeline.
- Integrate Ruff, Semgrep, SonarQube, and Trivy into one security workflow.
- Convert scanner outputs into SonarQube external issues where applicable.
- Normalize, deduplicate, group, and prioritize security findings.
- Apply security quality gate and release gate logic.
- Use LangChain and an LLM to produce contextual security advisory output.
- Publish advisory results as HTML reports, JSON artifacts, and Telegram notifications.

The LLM is not used as the primary vulnerability detector. Detection remains the responsibility of deterministic security tools. The LLM acts as an analytical layer that explains risk, summarizes context, and recommends remediation actions based on consolidated findings.

## Research Contribution

This repository supports an applied research prototype with the following contributions:

| Contribution | Description |
| --- | --- |
| Multi-tool DevSecOps pipeline | Integrates code quality, SAST, static analysis, and vulnerability scanning in a single Jenkins flow. |
| Finding normalization | Converts heterogeneous scanner output into a consistent finding representation. |
| Deduplication and prioritization | Reduces redundant findings and selects high-impact issues for downstream analysis. |
| AI-driven security analytics | Uses LangChain and LLM-based analysis to create contextual advisory output. |
| Release decision support | Produces risk status, release blockers, and release recommendation for CI/CD gating. |

## Architecture

```mermaid
flowchart TD
    A[Source Code Repository] --> B[Jenkins Agent-Based Pipeline]
    B --> C[Ruff Lint and Static Checks]
    B --> D[Semgrep SAST]
    B --> E[Build Container Image]
    E --> F[Trivy Vulnerability Scan]
    C --> G[External Issues Consolidation]
    D --> G
    F --> G
    G --> H[SonarQube Analysis and Quality Gate]
    H --> I[Finding Normalization]
    I --> J[Deduplication, Grouping, and Prioritization]
    J --> K[LangChain and LLM Security Analytics]
    K --> L[Security Advisory Report]
    L --> M[Release Gate Decision]
    L --> N[HTML Report, JSON Artifact, Telegram Notification]
```

## Pipeline Flow

The service pipeline in `ci-cd/pipelines/services/payment-service.groovy` executes the following stages:

1. Initialize pipeline context from webhook or upstream job parameters.
2. Checkout source code by tag and capture workspace baseline.
3. Run Ruff for Python linting and static checks.
4. Run Semgrep with security-audit, OWASP Top Ten, and CWE Top 25 rulesets.
5. Verify workspace integrity.
6. Build and push container image.
7. Run Trivy against the built image.
8. Consolidate Ruff, Semgrep, and Trivy outputs into SonarQube external issue payloads.
9. Run SonarQube code quality and quality gate analysis.
10. Run AI-driven security advisory generation.
11. Publish advisory report and send Telegram notification.
12. Deploy only when the advisory recommendation allows release.

## Repository Structure

```text
.
|-- ai-advisory-platform/        # Python advisory service using LangChain and LLM integration
|-- ci-cd/
|   |-- jenkins-shared-library/  # Jenkins shared library for scanners, gates, and notifications
|   `-- pipelines/               # Jenkins pipeline definitions
|-- config/                      # CI, environment, severity, SLA, and gate policy examples
|-- docs/                        # Architecture diagrams and ADR documents
|-- example/                     # Example applications, workflows, and AI experiments
`-- infrastructure/              # Jenkins, SonarQube, PostgreSQL, Nginx report portal, GitOps assets
```

## Core Components

| Component | Role |
| --- | --- |
| Jenkins | Main CI/CD orchestrator using agent-based execution. |
| Ruff | Python linting and selected static checks. |
| Semgrep | SAST scanner using security-focused rulesets. |
| Trivy | Container and vulnerability scanner. |
| SonarQube | Code quality analysis, external issue aggregation, and quality gate. |
| AI Advisory Platform | Python service that analyzes SonarQube findings and generates advisory reports. |
| LangChain | Orchestrates prompt construction and LLM interaction. |
| LLM | Generates contextual risk interpretation and remediation guidance. |
| Telegram | Sends pipeline and advisory summary notifications. |
| Nginx Report Portal | Serves generated HTML security advisory reports. |

## AI Advisory Platform

The AI advisory service is located in `ai-advisory-platform/`. It fetches SonarQube findings, normalizes and prioritizes them, routes the request to the selected analysis chain, and writes both HTML and JSON advisory output.

Supported analysis modes are defined in the application constants and prompts. The Jenkins shared library currently invokes:

```bash
python -m app.main \
  --project-key payment-service \
  --analysis-mode critical_analysis \
  --sonar-url http://sonarqube:9000 \
  --output-file advisory_report.json \
  --report-dir payment-service-critical-analysis
```

For local development:

```bash
cd ai-advisory-platform
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
make run
```

Required environment values, including LLM credentials and SonarQube connectivity, should be provided through the local `.env` files or runtime environment variables.

## Infrastructure

The infrastructure folder contains Docker-based services for the research prototype, including Jenkins, SonarQube, PostgreSQL, and an Nginx report portal.

```bash
cd infrastructure
docker compose --env-file .env up -d --build
```

Before running, review `infrastructure/docker-compose.yml` and `infrastructure/.env`. Several volume paths are environment-specific and may need to be adjusted for a different workstation or server.

## Security Policies and Gate Logic

Policy and tool metadata are maintained in the Jenkins shared library resources:

- `ci-cd/jenkins-shared-library/resources/tool-metadata.yaml`
- `ci-cd/jenkins-shared-library/resources/security-policy.yaml`
- `ci-cd/jenkins-shared-library/resources/severity-map.yaml`
- `ci-cd/jenkins-shared-library/resources/sla-policy.yaml`

Production release logic is designed to block deployment when critical risk remains unresolved. In the current service pipeline, deployment proceeds only when the advisory status is `SAFE_TO_DEPLOY`.

## Research Validation Snapshot

The journal experiment used a `payment-service` test case containing intentional security weaknesses. The framework produced the following results:

| Metric | Result |
| --- | ---: |
| Integrated tools | 4 tools: Ruff, Semgrep, SonarQube, Trivy |
| Raw findings | 160 |
| Normalized findings | 160 |
| Unique findings after deduplication | 37 |
| Redundant entries consolidated | 123 |
| Redundancy reduction | 76.88% |
| Critical or Major findings | 27 |
| Top findings sent to LLM | 15 |
| Release blockers | 14 |
| Overall risk | Critical |
| Exploitation risk | Critical |
| Release recommendation | Hold |

These results show that the framework can transform fragmented scanner outputs into structured security context, reduce redundant representation, prioritize high-impact findings, and generate advisory output that supports remediation and release decisions.

## Generated Outputs

The pipeline and advisory platform produce:

- SonarQube external issue reports.
- Consolidated security finding payloads.
- Security Advisory JSON output.
- HTML Security Advisory Report.
- Telegram notification summary.
- Jenkins build artifacts and execution logs.

## Limitations

- LLM output is advisory support, not an independent source of vulnerability truth.
- Advisory quality depends on prompt design, model behavior, input context, and scanner coverage.
- The current experiment is validated mainly against a controlled `payment-service` scenario.
- Additional validation is recommended across more languages, architectures, deployment environments, and real-world application portfolios.

## References in This Repository

- AI advisory implementation: `ai-advisory-platform/`
- Jenkins shared library: `ci-cd/jenkins-shared-library/`
- Payment service pipeline: `ci-cd/pipelines/services/payment-service.groovy`
- Architecture diagrams: `docs/architecture/`
- Infrastructure stack: `infrastructure/`
