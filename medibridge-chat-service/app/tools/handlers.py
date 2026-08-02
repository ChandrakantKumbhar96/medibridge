"""Executes a tool call the model requested. Kept separate from specs.py so
the schema (what the model sees) and the implementation (what actually runs)
can be read independently.

Every handler takes (user_id, role) so dispatch() can call any of them the
same way, even ones like get_policies that ignore both - user_id/role come
from the caller's verified identity (see routes_chat.py), never from the
model's tool-call arguments.
"""
from app.clients import spring_client

_HANDLERS = {
    "get_policies": lambda user_id, role: spring_client.get_policies(),
    "get_appointment_count": spring_client.get_appointment_count,
    "get_next_appointment": spring_client.get_next_appointment,
    "get_today_queue_count": spring_client.get_today_queue_count,
    "get_reschedule_status": spring_client.get_reschedule_status,
    "get_refund_status": spring_client.get_refund_status,
    "get_prescription_status": spring_client.get_prescription_status,
}


def dispatch(
    name: str,
    _arguments: dict,
    *,
    user_id: str | None = None,
    role: str | None = None,
) -> dict:
    handler = _HANDLERS.get(name)
    if handler is None:
        return {"error": f"Unknown tool '{name}'"}
    try:
        return handler(user_id, role)
    except Exception as exc:
        # A tool failure should degrade to "I don't know", not a 500 - the
        # caller is a patient asking a question, not a service integration.
        return {"error": f"Could not fetch this right now: {exc}"}
