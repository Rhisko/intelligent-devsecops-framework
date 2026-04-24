from __future__ import annotations

from pydantic import BaseModel, Field
from typing import Literal



# class CriticalAnalysisOutput(BaseModel):
#     project: str
#     overall_risk: str
#     critical_findings_summary: list[str] = Field(default_factory=list)
#     exploitation_risk: str
#     recommended_actions: list[str] = Field(default_factory=list)
class CriticalFindingRow(BaseModel):
    finding_id: str = Field(description="Stable identifier for the finding")
    component: str = Field(description="Affected file, manifest, or artifact")
    line: int | None = Field(default=None, description="Line number if applicable")
    severity: str = Field(description="Normalized severity")
    issue_type: str = Field(description="Issue type such as VULNERABILITY, BUG, CODE_SMELL")
    engine: str = Field(description="Primary detection source such as sonar, semgrep, ruff, trivy")
    title: str = Field(description="Short human-readable finding title")
    risk_summary: str = Field(description="One-sentence explanation of why this issue matters")
    exploitability: Literal["HIGH", "MEDIUM", "LOW", "UNKNOWN"] = Field(description="How practical exploitation is")
    blast_radius: Literal["HIGH", "MEDIUM", "LOW", "UNKNOWN"] = Field(description="Potential impact scope if exploited")
    confidence: Literal["HIGH", "MEDIUM", "LOW"] = Field(description="Confidence in the assessment")
    remediation_priority: Literal["P1", "P2", "P3"] = Field(description="Priority for remediation")
    recommended_fix: str = Field(description="Concrete fix recommendation")
    developer_action: str = Field(description="Actionable next step for developer")
    release_blocker: bool = Field(description="Whether this finding should block release")
    rationale: str = Field(description="Why this issue got this priority")


class CriticalAnalysisOutput(BaseModel):
    project: str
    overall_risk: Literal["CRITICAL", "HIGH", "MEDIUM", "LOW"]
    exploitation_risk: Literal["CRITICAL", "HIGH", "MEDIUM", "LOW"]
    release_recommendation: Literal["HOLD", "REVIEW", "GO"]
    executive_summary: str = Field(description="Short summary for security lead or manager")
    findings: list[CriticalFindingRow] = Field(default_factory=list)
    immediate_actions: list[str] = Field(default_factory=list)