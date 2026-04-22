from enum import StrEnum


class AnalysisMode(StrEnum):
    SUMMARY = "summary"
    CRITICAL_ANALYSIS = "critical_analysis"
    DEPENDENCY_ANALYSIS = "dependency_analysis"
    RELEASE_GATE = "release_gate"
