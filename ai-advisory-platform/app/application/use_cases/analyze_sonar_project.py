from __future__ import annotations

from app.application.services.advisory_service import AdvisoryService
from app.application.services.preprocessing_service import PreprocessingService
from app.application.services.risk_scoring_service import RiskScoringService
from app.application.services.routing_service import RoutingService
from app.infrastructure.clients.sonar_client import SonarClient
from app.infrastructure.llm.langchain.model_router import ModelRouter


class AnalyzeSonarProjectUseCase:
    def __init__(self, sonar_client: SonarClient, model_router: ModelRouter, max_top_findings: int = 10):
        self.sonar_client = sonar_client
        self.preprocessing_service = PreprocessingService()
        self.risk_scoring_service = RiskScoringService()
        self.routing_service = RoutingService()
        self.advisory_service = AdvisoryService(max_findings=max_top_findings)
        self.model_router = model_router

    def execute(self, project_key: str, analysis_mode: str) -> dict:
        issues = self.sonar_client.fetch_project_issues(project_key)
        findings, risk_profile, grouped = self.preprocessing_service.preprocess(issues)
        findings = self.risk_scoring_service.top_findings(findings)
        context = self.advisory_service.build_context(project_key, findings, risk_profile, grouped)

        task_name = self.routing_service.select_task(
            analysis_mode=analysis_mode,
            has_critical=risk_profile.critical_count > 0,
            has_dependency_findings=any(f['family'] == 'dependency_vulnerability' for f in context['top_findings']),
        )
        print(f'Selected task: {task_name}')
        return self.model_router.run(task_name=task_name, payload=context)
