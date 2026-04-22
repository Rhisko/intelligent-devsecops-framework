from __future__ import annotations

from typing import List
from pydantic import BaseModel, Field

from app.domain.enums.issue_family import IssueFamily
from app.domain.enums.issue_type import IssueType
from app.domain.enums.severity import Severity


class Finding(BaseModel):
    canonical_id: str
    project: str
    path: str
    line: int
    family: IssueFamily
    type: IssueType
    severity: Severity
    title: str
    primary_message: str
    engines: List[str] = Field(default_factory=list)
    rules: List[str] = Field(default_factory=list)
    messages: List[str] = Field(default_factory=list)
    source_issue_keys: List[str] = Field(default_factory=list)
    effort_minutes: int = 0
    score: int = 0
