from __future__ import annotations

from app.domain.enums.issue_family import IssueFamily
from app.domain.enums.severity import Severity
from app.domain.policies.release_gate_policy import baseline_release_recommendation


class AdvisoryService:
    def __init__(self, max_findings: int = 10, max_grouped_paths: int | None = None, max_findings_per_path: int = 5):
        self.max_findings = max_findings
        self.max_grouped_paths = max_grouped_paths or max_findings
        self.max_findings_per_path = max_findings_per_path

    def build_context(self, project_key: str, findings: list, risk_profile, grouped_findings: dict[str, list]) -> dict:
        dependency_findings = [f for f in findings if f.family == IssueFamily.DEPENDENCY_VULNERABILITY]
        critical_findings = [f for f in findings if f.severity in {Severity.CRITICAL, Severity.MAJOR}]
        limited_critical_findings = critical_findings[:self.max_findings]
        limited_dependency_findings = dependency_findings[:self.max_findings]
        limited_top_findings = findings[:self.max_findings]
        limited_grouped_paths = {
            k: [f.model_dump() for f in v[:self.max_findings_per_path]]
            for k, v in list(grouped_findings.items())[:self.max_grouped_paths]
        }

        print(f'Total critical/major findings: {len(critical_findings)}')
        for f in critical_findings:
            print(f'Critical/Major Finding: {f.path} - Severity: {f.severity} - Score: {f.score} - family: {f.family}')
        print(
            'Context sent to LLM: '
            f'critical_findings={len(limited_critical_findings)}, '
            f'dependency_findings={len(limited_dependency_findings)}, '
            f'top_findings={len(limited_top_findings)}, '
            f'grouped_paths={len(limited_grouped_paths)}'
        )
        return {
            'project': project_key,
            'summary': risk_profile.model_dump(),
            'input_counts': {
                'critical_findings': len(limited_critical_findings),
                'dependency_findings': len(limited_dependency_findings),
                'top_findings': len(limited_top_findings),
                'grouped_paths': len(limited_grouped_paths),
            },
            'release_baseline': baseline_release_recommendation(risk_profile.critical_count, risk_profile.major_count),
            'critical_findings': [f.model_dump() for f in limited_critical_findings],
            'dependency_findings': [f.model_dump() for f in limited_dependency_findings],
            'top_findings': [f.model_dump() for f in limited_top_findings],
            'grouped_paths': limited_grouped_paths,
        }
