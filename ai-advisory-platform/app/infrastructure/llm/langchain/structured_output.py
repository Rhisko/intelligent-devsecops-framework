from __future__ import annotations

from typing import Type

from langchain_openai import ChatOpenAI


class StructuredOutputFactory:
    def build(self, model_name: str, schema: Type, temperature: float = 0.0):
        llm = ChatOpenAI(model=model_name, temperature=temperature)
        return llm.with_structured_output(schema)
