from __future__ import annotations

from typing import Any
from pydantic import BaseModel


class AdvisoryResponse(BaseModel):
    project: str
    analysis_mode: str
    result: dict[str, Any]
