from __future__ import annotations

from app.domain.models.finding import Finding


class RiskScoringService:
    def top_findings(self, findings: list[Finding], limit: int = 100) -> list[Finding]:
        return findings[:limit]
