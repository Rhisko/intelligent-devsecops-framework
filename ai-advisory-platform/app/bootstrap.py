from __future__ import annotations

import os
from pathlib import Path

import yaml
from dotenv import load_dotenv

from app.application.use_cases.analyze_sonar_project import AnalyzeSonarProjectUseCase
from app.infrastructure.clients.sonar_client import SonarClient
from app.infrastructure.llm.langchain.model_router import ModelRouter
from app.shared.exceptions import ConfigurationError


class Bootstrap:
    def __init__(self, project_root: str | None = None):
        self.project_root = Path(project_root or Path(__file__).resolve().parents[1])
        load_dotenv(self.project_root / '.env')

    def build_analyze_sonar_project_use_case(self, sonar_url_override: str | None = None) -> AnalyzeSonarProjectUseCase:
        app_config = yaml.safe_load((self.project_root / 'configs' / 'app.yaml').read_text(encoding='utf-8'))
        sonar_url = sonar_url_override or os.getenv('SONAR_URL')
        sonar_token = os.getenv('SONAR_TOKEN')

        if not sonar_url:
            raise ConfigurationError('SONAR_URL is required')
        if not sonar_token:
            raise ConfigurationError('SONAR_TOKEN is required')

        sonar_client = SonarClient(
            sonar_url=sonar_url,
            token=sonar_token,
            timeout_seconds=app_config.get('http_timeout_seconds', 30),
        )
        model_router = ModelRouter(str(self.project_root / 'configs' / 'model_routing.yaml'))
        return AnalyzeSonarProjectUseCase(sonar_client=sonar_client, model_router=model_router)
