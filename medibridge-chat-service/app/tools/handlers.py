"""Executes a tool call the model requested. Kept separate from specs.py so
the schema (what the model sees) and the implementation (what actually runs)
can be read independently.
"""
from app.clients import spring_client

_HANDLERS = {
    "get_policies": spring_client.get_policies,
}


def dispatch(name: str, _arguments: dict) -> dict:
    handler = _HANDLERS.get(name)
    if handler is None:
        return {"error": f"Unknown tool '{name}'"}
    try:
        return handler()
    except Exception as exc:
        # A tool failure should degrade to "I don't know", not a 500 - the
        # caller is a patient asking a question, not a service integration.
        return {"error": f"Could not fetch this right now: {exc}"}
