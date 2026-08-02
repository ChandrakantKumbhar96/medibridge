import json
import re

from fastapi import APIRouter, Depends

from app.auth.internal_key import require_internal_key
from app.guardrails.emergency_rules import EMERGENCY_MESSAGE, is_emergency
from app.guardrails.specialty_rules import match_specialty
from app.llm.groq_client import complete_chat_with_tools
from app.llm.prompts import TRIAGE_SYSTEM_PROMPT
from app.models.triage import TriageAnswerRequest, TriageResult
from app.session import redis_store
from app.tools.handlers import dispatch
from app.tools.specs import TOOLS

# Only the gateway may call this directly - see require_internal_key.
router = APIRouter(prefix="/triage", tags=["triage"], dependencies=[Depends(require_internal_key)])

_CODE_FENCE = re.compile(r"```(?:json)?\s*(\{.*\})\s*```", re.DOTALL)


def _parse_llm_result(raw: str) -> TriageResult:
    fenced = _CODE_FENCE.search(raw)
    payload = fenced.group(1) if fenced else raw
    try:
        return TriageResult(**json.loads(payload))
    except (json.JSONDecodeError, TypeError, ValueError):
        # Malformed LLM output degrades to "ask again" rather than a 500.
        return TriageResult(needs_more_info=True, next_question=raw)


@router.post("/answer", response_model=TriageResult)
def answer(request: TriageAnswerRequest) -> TriageResult:
    if is_emergency(request.message):
        redis_store.clear(request.session_id)
        return TriageResult(urgency="emergency", summary=EMERGENCY_MESSAGE, needs_more_info=False)

    history = redis_store.load_history(request.session_id)
    history.append({"role": "user", "content": request.message})

    # Deterministic rules run before the LLM ever sees the symptoms, checked
    # against everything said this session - a keyword may only show up on
    # a later turn, not necessarily the first message.
    combined_text = " ".join(turn["content"] for turn in history if turn["role"] == "user")
    matched = match_specialty(combined_text)
    if matched:
        redis_store.clear(request.session_id)
        return TriageResult(
            specialty=matched["specialty"],
            urgency=matched["urgency"],
            summary=f"Based on what you've described, we recommend booking {matched['specialty']}.",
            needs_more_info=False,
        )

    raw = complete_chat_with_tools(TRIAGE_SYSTEM_PROMPT, history, TOOLS, dispatch)
    result = _parse_llm_result(raw)

    if result.needs_more_info:
        history.append({"role": "assistant", "content": raw})
        redis_store.save_history(request.session_id, history)
    else:
        redis_store.clear(request.session_id)

    return result
