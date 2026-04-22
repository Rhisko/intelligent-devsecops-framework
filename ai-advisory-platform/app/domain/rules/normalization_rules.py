from __future__ import annotations

from app.domain.enums.issue_type import IssueType
from app.domain.enums.severity import Severity


def normalize_severity(value: str) -> Severity:
    value = (value or 'INFO').upper()
    if value == 'CRITICAL':
        return Severity.CRITICAL
    if value in {'MAJOR', 'HIGH', 'BLOCKER'}:
        return Severity.MAJOR
    if value in {'MINOR', 'MEDIUM', 'LOW'}:
        return Severity.MINOR
    return Severity.INFO


def normalize_issue_type(value: str) -> IssueType:
    value = (value or '').upper()
    if value in IssueType.__members__:
        return IssueType[value]
    return IssueType.UNKNOWN


def extract_engine(issue: dict) -> str:
    return issue.get('externalRuleEngine') or 'sonar'
