from enum import StrEnum


class IssueType(StrEnum):
    VULNERABILITY = "VULNERABILITY"
    BUG = "BUG"
    CODE_SMELL = "CODE_SMELL"
    SECURITY_HOTSPOT = "SECURITY_HOTSPOT"
    UNKNOWN = "UNKNOWN"
