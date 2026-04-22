from __future__ import annotations

from pydantic import BaseModel, Field


class CriticalAnalysisOutput(BaseModel):
    project: str
    overall_risk: str
    critical_findings_summary: list[str] = Field(default_factory=list)
    exploitation_risk: str
    recommended_actions: list[str] = Field(default_factory=list)
