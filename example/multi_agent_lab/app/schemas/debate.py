from pydantic import BaseModel

class ProposalOutput(BaseModel):
    argument: str

class CritiqueOutput(BaseModel):
    critique: str
    score: float  # 1-10

class ModeratorOutput(BaseModel):
    decision: str  # "accept" or "revise"
    reason: str