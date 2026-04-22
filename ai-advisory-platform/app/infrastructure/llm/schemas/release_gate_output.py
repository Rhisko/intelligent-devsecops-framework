from __future__ import annotations

from pydantic import BaseModel, Field


class ReleaseGateOutput(BaseModel):
    project: str
    overall_risk: str
    release_recommendation: str
    release_rationale: str
    top_findings: list[str] = Field(default_factory=list)
    recommended_actions: list[str] = Field(default_factory=list)
