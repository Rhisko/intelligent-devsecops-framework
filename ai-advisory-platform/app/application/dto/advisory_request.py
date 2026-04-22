from __future__ import annotations

from pydantic import BaseModel


class AdvisoryRequest(BaseModel):
    project_key: str
    analysis_mode: str
    sonar_url: str | None = None
