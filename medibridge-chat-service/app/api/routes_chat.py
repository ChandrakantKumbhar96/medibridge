from fastapi import APIRouter, Depends

from app.auth.internal_key import require_internal_key
from app.config import settings
from app.guardrails.emergency_rules import EMERGENCY_MESSAGE, is_emergency
from app.llm.groq_client import complete_with_tools
from app.llm.prompts import FAQ_SYSTEM_PROMPT
from app.models.chat import ChatMessageRequest, ChatMessageResponse
from app.tools.handlers import dispatch
from app.tools.specs import tools_for

# Only the gateway may call this directly - see require_internal_key.
router = APIRouter(prefix="/chat", tags=["chat"], dependencies=[Depends(require_internal_key)])


@router.post("/message", response_model=ChatMessageResponse)
def send_message(request: ChatMessageRequest) -> ChatMessageResponse:
    if is_emergency(request.message):
        return ChatMessageResponse(answer=EMERGENCY_MESSAGE, chunks=[])

    def dispatch_for_caller(name: str, arguments: dict) -> dict:
        return dispatch(name, arguments, user_id=request.user_id, role=request.role)

    tools = tools_for(request.role)
    answer = complete_with_tools(
        FAQ_SYSTEM_PROMPT, request.message, tools, dispatch_for_caller, model=settings.groq_model_fast
    )
    return ChatMessageResponse(answer=answer, chunks=[])
