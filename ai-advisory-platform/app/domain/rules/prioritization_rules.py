from __future__ import annotations

from app.domain.enums.issue_family import IssueFamily
from app.domain.models.finding import Finding
from app.domain.policies.severity_policy import SEVERITY_SCORES


def score_finding(finding: Finding) -> int:
    score = SEVERITY_SCORES[finding.severity]
    if len(finding.engines) > 1:
        score += 15
    if finding.family == IssueFamily.DEPENDENCY_VULNERABILITY:
        score += 20
    if finding.family in {
        IssueFamily.SQL_INJECTION,
        IssueFamily.HARDCODED_SECRET,
        IssueFamily.UNSAFE_EVAL,
        IssueFamily.INSECURE_HASH,
        IssueFamily.CONTAINER_RUNS_AS_ROOT,
    }:
        score += 10
    return score


def sort_findings(findings: list[Finding]) -> list[Finding]:
    for finding in findings:
        finding.score = score_finding(finding)
    return sorted(findings, key=lambda f: (-f.score, f.path, f.line))
