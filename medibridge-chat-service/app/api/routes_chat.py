from fastapi import APIRouter, Depends

from app.auth.internal_key import require_internal_key
from app.guardrails.emergency_rules import EMERGENCY_MESSAGE, is_emergency
from app.llm.groq_client import complete_with_tools
from app.llm.prompts import FAQ_SYSTEM_PROMPT
from app.models.chat import ChatMessageRequest, ChatMessageResponse
from app.rag.retriever import retrieve
from app.tools.handlers import dispatch
from app.tools.specs import TOOLS

# Only the gateway may call this directly - see require_internal_key.
router = APIRouter(prefix="/chat", tags=["chat"], dependencies=[Depends(require_internal_key)])


@router.post("/message", response_model=ChatMessageResponse)
def send_message(request: ChatMessageRequest) -> ChatMessageResponse:
    if is_emergency(request.message):
        return ChatMessageResponse(answer=EMERGENCY_MESSAGE, chunks=[])

    chunks = retrieve(request.message)
    if not chunks:
        return ChatMessageResponse(answer=None, chunks=[])

    context = "\n\n".join(f"[{c.source}] {c.text}" for c in chunks)
    answer = complete_with_tools(
        FAQ_SYSTEM_PROMPT,
        f"Context:\n{context}\n\nQuestion: {request.message}",
        TOOLS,
        dispatch,
    )
    return ChatMessageResponse(answer=answer, chunks=chunks)
