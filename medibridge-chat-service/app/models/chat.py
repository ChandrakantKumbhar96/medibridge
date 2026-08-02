from pydantic import BaseModel


class ChatMessageRequest(BaseModel):
    session_id: str
    message: str


class RetrievedChunk(BaseModel):
    source: str
    text: str
    score: float


class ChatMessageResponse(BaseModel):
    # None only when retrieval finds nothing to ground an answer in - the
    # emergency guardrail and normal Groq synthesis both set a real string.
    answer: str | None = None
    chunks: list[RetrievedChunk]
