from __future__ import annotations

from collections import defaultdict

from app.domain.models.finding import Finding


def group_findings_by_path(findings: list[Finding]) -> dict[str, list[Finding]]:
    grouped: dict[str, list[Finding]] = defaultdict(list)
    for finding in findings:
        grouped[finding.path].append(finding)
    return dict(grouped)
