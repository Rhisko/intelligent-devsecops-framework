from __future__ import annotations

from app.domain.enums.severity import Severity


SEVERITY_SCORES = {
    Severity.CRITICAL: 100,
    Severity.MAJOR: 70,
    Severity.MINOR: 30,
    Severity.INFO: 10,
}
