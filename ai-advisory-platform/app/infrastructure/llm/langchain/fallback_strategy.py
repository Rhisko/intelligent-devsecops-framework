from __future__ import annotations

from typing import Type

from app.infrastructure.llm.langchain.structured_output import StructuredOutputFactory


class FallbackStrategy:
    def __init__(self):
        self.factory = StructuredOutputFactory()

    def build_with_fallback(self, primary_model: str, fallback_model: str, schema: Type):
        primary = self.factory.build(primary_model, schema)
        fallback = self.factory.build(fallback_model, schema)
        return primary.with_fallbacks([fallback])
