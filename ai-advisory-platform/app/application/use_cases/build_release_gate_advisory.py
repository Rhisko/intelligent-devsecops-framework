from __future__ import annotations

from app.application.services.advisory_service import AdvisoryService
from app.application.services.preprocessing_service import PreprocessingService
from app.application.services.risk_scoring_service import RiskScoringService
from app.application.services.routing_service import RoutingService
from app.infrastructure.llm.langchain.model_router import ModelRouter


class BuildReleaseGateAdvisoryUseCase:
    def __init__(self, model_router: ModelRouter):
        self.preprocessing_service = PreprocessingService()
        self.risk_scoring_service = RiskScoringService()
        self.routing_service = RoutingService()
        self.advisory_service = AdvisoryService()
        self.model_router = model_router

    def execute(self, project_key: str, issues: list):
        findings, risk_profile, grouped = self.preprocessing_service.preprocess(issues)
        findings = self.risk_scoring_service.top_findings(findings)
        context = self.advisory_service.build_context(project_key, findings, risk_profile, grouped)
        task = self.routing_service.select_task(
            analysis_mode='release_gate',
            has_critical=risk_profile.critical_count > 0,
            has_dependency_findings=any(f['family'] == 'dependency_vulnerability' for f in context['top_findings']),
        )
        return self.model_router.run(task_name=task, payload=context)
