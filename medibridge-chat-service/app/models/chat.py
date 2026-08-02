from pydantic import BaseModel


class ChatMessageRequest(BaseModel):
    session_id: str
    message: str
    # Set by the gateway from the caller's verified JWT (see chat.routes.js) -
    # never trust these if a client could set them directly.
    user_id: str | None = None
    role: str | None = None


class RetrievedChunk(BaseModel):
    source: str
    text: str
    score: float


class ChatMessageResponse(BaseModel):
    # None only when retrieval finds nothing to ground an answer in - the
    # emergency guardrail and normal Groq synthesis both set a real string.
    answer: str | None = None
    chunks: list[RetrievedChunk]
