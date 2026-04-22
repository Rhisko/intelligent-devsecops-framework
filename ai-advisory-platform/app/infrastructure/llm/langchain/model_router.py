from __future__ import annotations

from pathlib import Path

import yaml

from app.infrastructure.llm.langchain.chains.critical_analysis_chain import CriticalAnalysisChain
from app.infrastructure.llm.langchain.chains.dependency_analysis_chain import DependencyAnalysisChain
from app.infrastructure.llm.langchain.chains.release_gate_chain import ReleaseGateChain
from app.infrastructure.llm.langchain.chains.summary_chain import SummaryChain


class ModelRouter:
    def __init__(self, config_path: str = 'configs/model_routing.yaml'):
        self.config_path = Path(config_path)
        self._config = yaml.safe_load(self.config_path.read_text(encoding='utf-8'))
        self._chains = {
            'summary': SummaryChain(**self._task_models('summary')),
            'critical_analysis': CriticalAnalysisChain(**self._task_models('critical_analysis')),
            'dependency_analysis': DependencyAnalysisChain(**self._task_models('dependency_analysis')),
            'release_gate': ReleaseGateChain(**self._task_models('release_gate')),
        }

    def _task_models(self, task_name: str) -> dict:
        cfg = self._config[task_name]
        return {'primary_model': cfg['primary_model'], 'fallback_model': cfg['fallback_model']}

    def run(self, task_name: str, payload: dict) -> dict:
        chain = self._chains[task_name]
        print(f'Running chain for task: {task_name} with payload keys: {list(payload.keys())}')
        return chain.run(payload)
