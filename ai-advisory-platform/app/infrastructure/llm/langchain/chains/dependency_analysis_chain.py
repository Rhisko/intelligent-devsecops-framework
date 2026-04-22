from __future__ import annotations

import json

from app.infrastructure.llm.langchain.fallback_strategy import FallbackStrategy
from app.infrastructure.llm.langchain.prompt_loader import PromptLoader
from app.infrastructure.llm.schemas.dependency_analysis_output import DependencyAnalysisOutput


class DependencyAnalysisChain:
    def __init__(self, primary_model: str, fallback_model: str):
        self.prompt_loader = PromptLoader()
        self.model = FallbackStrategy().build_with_fallback(primary_model, fallback_model, DependencyAnalysisOutput)

    def run(self, payload: dict) -> dict:
        system_text, user_text = self.prompt_loader.load('dependency_analysis')
        prompt = [
            ('system', system_text),
            ('human', user_text + '\n\nPayload:\n' + json.dumps(payload, indent=2)),
        ]
        result = self.model.invoke(prompt)
        return result.model_dump()
