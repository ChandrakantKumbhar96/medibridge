"""Groq function-calling tool schemas (OpenAI-compatible) offered to the FAQ
assistant. The FAQ text in app/llm/prompts.py covers *what a feature is*;
tools cover *what the current numbers/records are* - the two are
complementary, not overlapping, so the system prompt tells the model when to
reach for one.

Descriptions here are kept deliberately short - all 29 schemas are resent on
every completion call (including every tool round-trip), and free-tier Groq
models cap out at a few thousand tokens per minute. A verbose description on
each of 29 tools is what pushed requests over that cap; keep new ones short
too; only mention "never guess" where a wrong guess is a real risk (a name,
date or figure), not on every tool.

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
            "Get current cancellation, reschedule, no-show and follow-up "
            "policy numbers - never quote one from memory."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_APPOINTMENT_COUNT = {
    "type": "function",
    "function": {
        "name": "get_appointment_count",
        "description": "Get how many upcoming appointments the caller has.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_NEXT_APPOINTMENT = {
    "type": "function",
    "function": {
        "name": "get_next_appointment",
        "description": "Get the caller's single next upcoming appointment - who and when.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_TODAY_QUEUE_COUNT = {
    "type": "function",
    "function": {
        "name": "get_today_queue_count",
        "description": "Get how many appointments a doctor has today.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_RESCHEDULE_STATUS = {
    "type": "function",
    "function": {
        "name": "get_reschedule_status",
        "description": (
            "Get how many times the patient has rescheduled their next "
            "appointment, the limit, and whether they can again."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_REFUND_STATUS = {
    "type": "function",
    "function": {
        "name": "get_refund_status",
        "description": "Get the amount and date of the patient's most recent refund - never guess.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_PRESCRIPTION_STATUS = {
    "type": "function",
    "function": {
        "name": "get_prescription_status",
        "description": "Get the patient's most recently issued prescription - doctor and date.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_UPCOMING_APPOINTMENTS = {
    "type": "function",
    "function": {
        "name": "get_upcoming_appointments",
        "description": "Get the patient's full list of upcoming appointments, not just the next one.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_APPOINTMENT_HISTORY = {
    "type": "function",
    "function": {
        "name": "get_appointment_history",
        "description": (
            "Get the patient's past/completed appointments, e.g. who their "
            "last doctor was - never guess a name from memory."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_MY_PROFILE = {
    "type": "function",
    "function": {
        "name": "get_my_profile",
        "description": (
            "Get the caller's own account profile - name, email, phone "
            "(plus specialization/fee for a doctor)."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_MY_STATS = {
    "type": "function",
    "function": {
        "name": "get_my_stats",
        "description": "Get the patient's dashboard summary counts (upcoming/completed appointments).",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_FAMILY_MEMBERS = {
    "type": "function",
    "function": {
        "name": "get_family_members",
        "description": "Get the family members (dependents) registered on the patient's account.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_MEDICAL_RECORDS = {
    "type": "function",
    "function": {
        "name": "get_medical_records",
        "description": "Get the medical reports/documents the patient has uploaded.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_SECOND_OPINIONS = {
    "type": "function",
    "function": {
        "name": "get_second_opinions",
        "description": "Get the patient's second opinion requests and their status.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_PRESCRIPTIONS = {
    "type": "function",
    "function": {
        "name": "get_prescriptions",
        "description": "Get the patient's full prescription history, not just the latest.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_PAYMENT_HISTORY = {
    "type": "function",
    "function": {
        "name": "get_payment_history",
        "description": "Get the patient's full payment/transaction history.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_WEEKLY_SCHEDULE = {
    "type": "function",
    "function": {
        "name": "get_weekly_schedule",
        "description": "Get the doctor's configured weekly availability (days/hours).",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_TREATED_PATIENTS = {
    "type": "function",
    "function": {
        "name": "get_treated_patients",
        "description": "Get the patients this doctor has treated, with last/next visit.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_DOCTOR_DASHBOARD = {
    "type": "function",
    "function": {
        "name": "get_doctor_dashboard",
        "description": (
            "Get the doctor's full appointment dashboard - today's queue, "
            "upcoming, pending notes, completed/no-show history."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_MY_REVIEWS = {
    "type": "function",
    "function": {
        "name": "get_my_reviews",
        "description": "Get the ratings and reviews patients have left for this doctor.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_EARNINGS_SUMMARY = {
    "type": "function",
    "function": {
        "name": "get_earnings_summary",
        "description": (
            "Get the doctor's earnings summary - pending, paid, lifetime, "
            "commission rate, payout cycle - never guess a figure."
        ),
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_EARNINGS_LIST = {
    "type": "function",
    "function": {
        "name": "get_earnings_list",
        "description": "Get the doctor's itemised earnings, one row per completed consultation.",
        "parameters": {"type": "object", "properties": {}},
    },
}

GET_PAYOUTS = {
    "type": "function",
    "function": {
        "name": "get_payouts",
        "description": "Get the doctor's payout batches - period, amount, paid or not.",
        "parameters": {"type": "object", "properties": {}},
    },
}

LIST_SPECIALTIES = {
    "type": "function",
    "function": {
        "name": "list_specialties",
        "description": "Get the list of medical specialties offered on the platform.",
        "parameters": {"type": "object", "properties": {}},
    },
}

SEARCH_DOCTORS = {
    "type": "function",
    "function": {
        "name": "search_doctors",
        "description": (
            "Search active doctors by specialty and/or name - never invent "
            "a doctor or specialty that wasn't returned."
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "specialization": {
                    "type": "string",
                    "description": "Specialty to filter by, e.g. 'Cardiologist'. Omit for all.",
                },
                "search": {
                    "type": "string",
                    "description": "Doctor name or keyword. Omit to browse by specialty alone.",
                },
            },
        },
    },
}

GET_DOCTOR_PROFILE = {
    "type": "function",
    "function": {
        "name": "get_doctor_profile",
        "description": (
            "Get one doctor's public profile by id, as returned by search_doctors."
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "doctor_id": {
                    "type": "string",
                    "description": "The doctor's id, from search_doctors.",
                },
            },
            "required": ["doctor_id"],
        },
    },
}

GET_NEXT_AVAILABLE_SLOT = {
    "type": "function",
    "function": {
        "name": "get_next_available_slot",
        "description": "Get the soonest open appointment slot in a specialty.",
        "parameters": {
            "type": "object",
            "properties": {
                "specialization": {
                    "type": "string",
                    "description": "Specialty to search within, e.g. 'Dermatologist'. Omit for all.",
                },
            },
        },
    },
}

_COMMON_TOOLS = [
    GET_POLICIES,
    GET_APPOINTMENT_COUNT,
    GET_NEXT_APPOINTMENT,
    LIST_SPECIALTIES,
    SEARCH_DOCTORS,
    GET_DOCTOR_PROFILE,
    GET_NEXT_AVAILABLE_SLOT,
]

_TOOLS_BY_ROLE = {
    "PATIENT": [
        *_COMMON_TOOLS,
        GET_RESCHEDULE_STATUS,
        GET_REFUND_STATUS,
        GET_PRESCRIPTION_STATUS,
        GET_UPCOMING_APPOINTMENTS,
        GET_APPOINTMENT_HISTORY,
        GET_MY_PROFILE,
        GET_MY_STATS,
        GET_FAMILY_MEMBERS,
        GET_MEDICAL_RECORDS,
        GET_SECOND_OPINIONS,
        GET_PRESCRIPTIONS,
        GET_PAYMENT_HISTORY,
    ],
    "DOCTOR": [
        *_COMMON_TOOLS,
        GET_TODAY_QUEUE_COUNT,
        GET_MY_PROFILE,
        GET_WEEKLY_SCHEDULE,
        GET_TREATED_PATIENTS,
        GET_DOCTOR_DASHBOARD,
        GET_MY_REVIEWS,
        GET_EARNINGS_SUMMARY,
        GET_EARNINGS_LIST,
        GET_PAYOUTS,
    ],
}


def tools_for(role: str | None) -> list[dict]:
    """Only offer the model tools it can actually fulfil for this caller -
    e.g. a doctor is never offered get_refund_status, a patient is never
    offered get_today_queue_count, and an identity-less caller (triage, or
    chat with no user_id/role) only gets get_policies.
    """
    return _TOOLS_BY_ROLE.get(role, [GET_POLICIES])
