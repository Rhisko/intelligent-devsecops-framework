from __future__ import annotations

from collections import defaultdict
from typing import Iterable

from app.domain.enums.severity import Severity
from app.domain.models.finding import Finding
from app.domain.models.issue import Issue

SEVERITY_ORDER = {
    Severity.CRITICAL: 4,
    Severity.MAJOR: 3,
    Severity.MINOR: 2,
    Severity.INFO: 1,
}


def build_canonical_id(issue: Issue) -> str:
    return f"{issue.family.value}:{issue.path}:{issue.line}"


def _highest_severity(issues: list[Issue]) -> Severity:
    return max((i.severity for i in issues), key=lambda sev: SEVERITY_ORDER[sev])


def merge_issues_to_findings(issues: Iterable[Issue]) -> list[Finding]:
    grouped: dict[str, list[Issue]] = defaultdict(list)
    print(f'Total raw issues before deduplication: {len(issues)}')
    for issue in issues:
        # print(f"Issue: {issue.family.value}")
        grouped[build_canonical_id(issue)].append(issue)
    
    print(f'Total unique findings after deduplication: {len(grouped)}')
    for canonical_id, grouped_issues in grouped.items():
        print(f"  {canonical_id}: {len(grouped_issues)} issues line number {grouped_issues[0].line} - Severity: {_highest_severity(grouped_issues)}")

    findings: list[Finding] = []
    for canonical_id, grouped_issues in grouped.items():
        first = grouped_issues[0]
        engines = sorted({i.engine for i in grouped_issues})
        rules = sorted({i.rule for i in grouped_issues})
        messages = [i.message for i in grouped_issues]
        findings.append(
            Finding(
                canonical_id=canonical_id,
                project=first.project,
                path=first.path,
                line=first.line,
                family=first.family,
                type=first.type,
                severity=_highest_severity(grouped_issues),
                title=first.family.value.replace('_', ' ').title(),
                primary_message=first.message,
                engines=engines,
                rules=rules,
                messages=messages,
                source_issue_keys=[i.issue_key for i in grouped_issues],
                effort_minutes=max(i.effort_minutes for i in grouped_issues),
            )
        )
    # print(f'Total findings created: {len(findings)}')
    return findings
