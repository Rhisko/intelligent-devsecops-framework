from __future__ import annotations

from pydantic import BaseModel, Field


class RiskProfile(BaseModel):
    total_raw_issues: int = 0
    total_normalized_issues: int = 0
    total_deduped_findings: int = 0
    critical_count: int = 0
    major_count: int = 0
    minor_count: int = 0
    info_count: int = 0
    top_risk_files: list[str] = Field(default_factory=list)
