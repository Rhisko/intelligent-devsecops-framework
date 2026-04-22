from __future__ import annotations

from pydantic import BaseModel, Field


class DependencyAnalysisOutput(BaseModel):
    project: str
    dependency_risk: str
    most_urgent_packages: list[str] = Field(default_factory=list)
    recommended_actions: list[str] = Field(default_factory=list)
