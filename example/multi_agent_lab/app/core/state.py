from pydantic import BaseModel
from typing import Optional

class DebateState(BaseModel):
    topic: str
    proposal: Optional[str] = None
    critique: Optional[str] = None
    score: Optional[float] = None
    round: int = 0