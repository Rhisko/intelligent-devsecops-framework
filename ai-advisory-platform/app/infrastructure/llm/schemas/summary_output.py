from __future__ import annotations

from pydantic import BaseModel, Field


class SummaryOutput(BaseModel):
    project: str
    overall_risk: str
    summary: str
    top_findings: list[str] = Field(default_factory=list)
