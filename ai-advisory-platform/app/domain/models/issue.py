from __future__ import annotations

from typing import List
from pydantic import BaseModel, Field

from app.domain.enums.issue_family import IssueFamily
from app.domain.enums.issue_type import IssueType
from app.domain.enums.severity import Severity


class Issue(BaseModel):
    issue_key: str
    project: str
    component: str
    path: str
    line: int = 1
    rule: str
    engine: str
    severity: Severity
    type: IssueType = IssueType.UNKNOWN
    message: str
    effort_minutes: int = 0
    impacts: List[dict] = Field(default_factory=list)
    is_external: bool = False
    family: IssueFamily = IssueFamily.UNKNOWN
