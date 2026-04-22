from __future__ import annotations

from app.domain.enums.issue_family import IssueFamily
from app.domain.policies.release_gate_policy import baseline_release_recommendation


class AdvisoryService:
    def build_context(self, project_key: str, findings: list, risk_profile, grouped_findings: dict[str, list]) -> dict:
        dependency_findings = [f for f in findings if f.family == IssueFamily.DEPENDENCY_VULNERABILITY]
        critical_findings = [f for f in findings if f.severity == 'CRITICAL']
        return {
            'project': project_key,
            'summary': risk_profile.model_dump(),
            'release_baseline': baseline_release_recommendation(risk_profile.critical_count, risk_profile.major_count),
            'critical_findings': [f.model_dump() for f in critical_findings[:10]],
            'dependency_findings': [f.model_dump() for f in dependency_findings[:10]],
            'top_findings': [f.model_dump() for f in findings[:15]],
            'grouped_paths': {k: [f.model_dump() for f in v[:5]] for k, v in list(grouped_findings.items())[:10]},
        }
