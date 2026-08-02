"""Groq function-calling tool schemas (OpenAI-compatible) offered to the FAQ
assistant. The FAQ text in app/llm/prompts.py covers *what a feature is*;
tools cover *what the current numbers/records are* - the two are
complementary, not overlapping, so the system prompt tells the model when to
reach for one.

Adding a tool means three things, all checked by name:
1. a schema here
2. a handler in app/tools/handlers.py
3. an entry in _TOOLS_BY_ROLE below, or it's built but never offered to
   anyone - see test_every_advertised_tool_has_a_registered_handler for the
   handler side of this, but nothing catches a forgotten role entry except
   noticing the model never calls it.
"""

GET_POLICIES = {
    "type": "function",
    "function": {
        "name": "get_policies",
        "description": (
            "Get MediBridge's current cancellation, reschedule, no-show and "
            "follow-up policy numbers (hours, percentages, limits). Call this "
            "whenever a patient asks how much refund they get, how many hours "
            "before an appointment counts as free cancellation, how many times "
            "they can reschedule, or whether a follow-up is free - never guess "
            "or quote a number from memory."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_APPOINTMENT_COUNT = {
    "type": "function",
    "function": {
        "name": "get_appointment_count",
        "description": (
            "Get how many upcoming appointments the current patient or doctor "
            "has. Call this whenever the caller asks how many appointments "
            "they have, whether they have any upcoming, or similar - never "
            "guess a number. Takes no arguments; it always answers for "
            "whoever is asking."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_NEXT_APPOINTMENT = {
    "type": "function",
    "function": {
        "name": "get_next_appointment",
        "description": (
            "Get the caller's single next upcoming appointment - who it's "
            "with and when. Call this for 'when is my next appointment' or "
            "'who am I seeing next', not for a count or a full list."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_TODAY_QUEUE_COUNT = {
    "type": "function",
    "function": {
        "name": "get_today_queue_count",
        "description": (
            "Get how many appointments a doctor has today. Only meaningful "
            "for a doctor caller - call this when a doctor asks how many "
            "patients they have today or how full today's schedule is."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_RESCHEDULE_STATUS = {
    "type": "function",
    "function": {
        "name": "get_reschedule_status",
        "description": (
            "Get how many times a patient has already rescheduled their "
            "next upcoming appointment, the policy limit, and whether they "
            "can reschedule again. Call this when a patient asks if they "
            "can still reschedule or how many reschedules they have left."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_REFUND_STATUS = {
    "type": "function",
    "function": {
        "name": "get_refund_status",
        "description": (
            "Get the amount and date of the patient's most recent refund. "
            "Call this when a patient asks whether their refund has been "
            "issued or how much they got back - never guess an amount."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_PRESCRIPTION_STATUS = {
    "type": "function",
    "function": {
        "name": "get_prescription_status",
        "description": (
            "Get the patient's most recently issued prescription - which "
            "doctor issued it and when. Call this when a patient asks if "
            "their prescription is ready or where their latest one is from."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

_COMMON_TOOLS = [GET_POLICIES, GET_APPOINTMENT_COUNT, GET_NEXT_APPOINTMENT]

_TOOLS_BY_ROLE = {
    "PATIENT": [*_COMMON_TOOLS, GET_RESCHEDULE_STATUS, GET_REFUND_STATUS, GET_PRESCRIPTION_STATUS],
    "DOCTOR": [*_COMMON_TOOLS, GET_TODAY_QUEUE_COUNT],
}


def tools_for(role: str | None) -> list[dict]:
    """Only offer the model tools it can actually fulfil for this caller -
    e.g. a doctor is never offered get_refund_status, a patient is never
    offered get_today_queue_count, and an identity-less caller (triage, or
    chat with no user_id/role) only gets get_policies.
    """
    return _TOOLS_BY_ROLE.get(role, [GET_POLICIES])
