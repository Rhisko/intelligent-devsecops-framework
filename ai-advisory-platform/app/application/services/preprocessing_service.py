from __future__ import annotations

from collections import Counter

from app.domain.enums.severity import Severity
from app.domain.models.issue import Issue
from app.domain.models.risk_profile import RiskProfile
from app.domain.rules.dedup_rules import merge_issues_to_findings
from app.domain.rules.grouping_rules import group_findings_by_path
from app.domain.rules.prioritization_rules import sort_findings


class PreprocessingService:
    def preprocess(self, issues: list[Issue]) -> tuple[list, RiskProfile, dict[str, list]]:
        findings = merge_issues_to_findings(issues)
        print(f'Total findings after preprocessing: {len(findings)}')
        findings = sort_findings(findings)
        print(f'Top findings after sorting: {len(findings)}')
        grouped = group_findings_by_path(findings)
        print(f'Total file groups after grouping: {len(grouped)}')

        counts = Counter(f.severity for f in findings)
        file_scores = Counter()
        for finding in findings:
            # print(f"Finding: {finding.path} - Severity: {finding.severity} - Score: {finding.score}")
            file_scores[finding.path] += finding.score
        top_risk_files = [path for path, _ in file_scores.most_common(5)]

        profile = RiskProfile(
            total_raw_issues=len(issues),
            total_normalized_issues=len(issues),
            total_deduped_findings=len(findings),
            critical_count=counts.get(Severity.CRITICAL, 0),
            major_count=counts.get(Severity.MAJOR, 0),
            minor_count=counts.get(Severity.MINOR, 0),
            info_count=counts.get(Severity.INFO, 0),
            top_risk_files=top_risk_files,
        )
        return findings, profile, grouped
