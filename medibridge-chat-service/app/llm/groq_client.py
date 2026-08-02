import json
from functools import lru_cache
from typing import Callable

from groq import Groq

from app.config import settings


@lru_cache
def get_client() -> Groq:
    return Groq(api_key=settings.groq_api_key)


def complete(system_prompt: str, user_message: str, model: str | None = None) -> str:
    response = get_client().chat.completions.create(
        model=model or settings.groq_model,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_message},
        ],
    )
    return response.choices[0].message.content


def _run_tool_round(
    messages: list[dict],
    tools: list[dict],
    dispatch: Callable[[str, dict], dict],
    resolved_model: str,
) -> str:
    """Runs one round-trip of tool-calling on top of a prepared message list.

    One round only, by design: these are FAQ/triage assistants answering from
    a handful of small lookups, not an agent chaining calls, so there is no
    loop back to the model after tool results other than the single final
    answer.
    """
    first = get_client().chat.completions.create(
        model=resolved_model,
        messages=messages,
        tools=tools,
        tool_choice="auto",
    )
    reply = first.choices[0].message

    if not reply.tool_calls:
        return reply.content

    messages.append(
        {
            "role": "assistant",
            "content": reply.content,
            "tool_calls": [
                {
                    "id": call.id,
                    "type": "function",
                    "function": {
                        "name": call.function.name,
                        "arguments": call.function.arguments,
                    },
                }
                for call in reply.tool_calls
            ],
        }
    )
    for call in reply.tool_calls:
        arguments = json.loads(call.function.arguments or "{}")
        result = dispatch(call.function.name, arguments)
        messages.append(
            {
                "role": "tool",
                "tool_call_id": call.id,
                "content": json.dumps(result),
            }
        )

    final = get_client().chat.completions.create(model=resolved_model, messages=messages)
    return final.choices[0].message.content


def complete_with_tools(
    system_prompt: str,
    user_message: str,
    tools: list[dict],
    dispatch: Callable[[str, dict], dict],
    model: str | None = None,
) -> str:
    """Like complete(), but lets the model call one round of tools first."""
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_message},
    ]
    return _run_tool_round(messages, tools, dispatch, model or settings.groq_model)


def complete_chat(system_prompt: str, history: list[dict], model: str | None = None) -> str:
    response = get_client().chat.completions.create(
        model=model or settings.groq_model,
        messages=[{"role": "system", "content": system_prompt}, *history],
    )
    return response.choices[0].message.content


def complete_chat_with_tools(
    system_prompt: str,
    history: list[dict],
    tools: list[dict],
    dispatch: Callable[[str, dict], dict],
    model: str | None = None,
) -> str:
    """Like complete_chat(), but lets the model call one round of tools first."""
    messages = [{"role": "system", "content": system_prompt}, *history]
    return _run_tool_round(messages, tools, dispatch, model or settings.groq_model)
