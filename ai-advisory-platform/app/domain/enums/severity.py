from enum import StrEnum


class Severity(StrEnum):
    BLOCKER = "CRITICAL"
    CRITICAL = "CRITICAL"
    MAJOR = "MAJOR"
    MINOR = "MINOR"
    INFO = "INFO"
