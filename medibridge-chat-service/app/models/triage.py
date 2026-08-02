from pydantic import BaseModel


class TriageAnswerRequest(BaseModel):
    session_id: str
    message: str


class TriageResult(BaseModel):
    specialty: str | None = None
    urgency: str = "routine"  # routine | urgent | emergency
    summary: str | None = None
    needs_more_info: bool = True
    next_question: str | None = None
