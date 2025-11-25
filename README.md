# Intelligent DevSecOps Automation Framework

### AI-Driven CI/CD Pipeline with Security Scanning, Policy-as-Code, and Automated Advisory Using Jenkins, n8n, SonarQube, and LLM

---

## **1. Overview**

The **Intelligent DevSecOps Automation Framework** is a fully automated, security-centric CI/CD ecosystem integrating:

- **Static Application Security Testing (SAST)**
- **Vulnerability Scanning**
- **Policy-as-Code Enforcement (OPA/Rego)**
- **AI-driven security advisory**
- **Workflow orchestration with n8n**
- **Multibranch CI/CD pipelines using Jenkins**

The framework is developed as part of a research thesis titled:

**"INTELLIGENT DEVSECOPS AUTOMATION FRAMEWORK:
INTEGRATING SONARQUBE, HORUSEC, TRIVY, LINTERS, AND CONFTEST POLICY-AS-CODE
WITH AI-DRIVEN SECURITY DETECTION & AUTOMATED ADVISORY
IN A JENKINS PIPELINE ORCHESTRATED BY n8n."**

---

## **2. Key Features**

### **2.1 Integrated DevSecOps Toolchain**

| Tool                     | Function                                                                |
| ------------------------ | ----------------------------------------------------------------------- |
| **Horusec**        | SAST scanner for multi-language vulnerabilities                         |
| **Trivy**          | Filesystem, container, IaC, and config scanner                          |
| **Linter Tools**   | Coding conventions enforcement                                          |
| **SonarQube**      | Centralized static analysis, vulnerability aggregation & quality gate   |
| **Conftest (OPA)** | Policy-as-Code enforcement                                              |
| **AI Module**      | LLM-powered automated advisory & remediation guidance                   |
| **n8n**            | Workflow orchestrator for notifications, gatekeeping & advisory routing |

---

## **3. System Architecture**

### **Security Data Flow — UPDATED**

Horusec, Trivy, and Linter results **are consolidated into SonarQube**,
then SonarQube dispatches analysis results to **n8n**,
and n8n triggers the **AI Advisory Engine (LLM)**.

```text
                                        ┌────────────────────────────┐
                                        │         Source Code        │
                                        │  (Microservices / Apps)    │
                                        └──────────────┬─────────────┘
                                                       │ Webhook
                                        ┌──────────────▼─────────────┐
                                        │        Jenkins CI/CD       │
                                        │   Multibranch Pipelines    │
                                        └───────┬─────────┬──────────┘
                                                │         │
                           ┌────────────────────┘         └──────────────────────┐
                           │                                                    │
                  ┌────────▼──────────┐                            ┌────────────▼──────────┐
                  │     Horusec       │                            │        Trivy          │
                  │ (SAST Analysis)   │                            │ Vulnerability Scanner │
                  └────────▲──────────┘                            └────────────▲──────────┘
                           │                                                    │
                           └──────────────────────┬─────────────────────────────┘
                                                  │
                                     ┌────────────▼───────────────┐
                                     │         Linter Tools        │
                                     │ (Standard Code Formatting)  │
                                     └────────────▲───────────────┘
                                                  │ Aggregated Output
                                                  ▼
                                      ┌────────────────────────────┐
                                      │        SonarQube           │
                                      │   Static Analysis Engine   │
                                      │  - Code Smells             │
                                      │  - Vulnerabilities         │
                                      │  - Security Hotspots       │
                                      │  - Quality Gate            │
                                      └────────────▲──────────────┘
                                                   │ Webhook/API
                                                   ▼
                           ┌──────────────────────────────────────────┐
                           │                  n8n                     │
                           │    DevSecOps Automation Orchestrator    │
                           │ - Receive Sonar Reports                  │
                           │ - Normalize & Enrich Data               │
                           │ - Trigger LLM Advisory Engine           │
                           │ - Approve/Reject Build (Gatekeeper)     │
                           └────────────────────▲────────────────────┘
                                                │ REST API Call
                                                ▼
                         ┌────────────────────────────────────────────┐
                         │      AI Security Advisory Engine (LLM)     │
                         │ - Vulnerability Explanation                │
                         │ - Remediation Recommendation               │
                         │ - Severity Prioritization                  │
                         │ - Developer-friendly Summary               │
                         └────────────────────────────────────────────┘
                                                │
                                                ▼
                                 ┌──────────────────────────────┐
                                 │ PR Comments / Slack / Jira   │
                                 │ Developer Notification        │
                                 └──────────────────────────────┘
```
