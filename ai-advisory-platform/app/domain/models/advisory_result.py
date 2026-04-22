from __future__ import annotations

from typing import Any
from pydantic import BaseModel, Field


class AdvisoryResult(BaseModel):
    project: str
    analysis_mode: str
    payload: dict[str, Any]
    raw_model: str | None = None
