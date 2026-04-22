from pydantic import BaseModel
from typing import List, Literal

class Issue(BaseModel):
    id: str
    severity: Literal["low", "medium", "high", "critical"]
    explanation: str
    recommended_fix: str

class SecurityAnalysis(BaseModel):
    risk_level: Literal["low", "medium", "high", "critical"]
    issues: List[Issue]
    overall_recommendation: str
