"""Executes a tool call the model requested. Kept separate from specs.py so
the schema (what the model sees) and the implementation (what actually runs)
can be read independently.

Every handler takes (arguments, user_id, role) so dispatch() can call any of
them the same way. user_id/role come from the caller's verified identity (see
routes_chat.py), never from the model's tool-call arguments - most handlers
ignore arguments entirely and only the discovery tools (search_doctors,
get_doctor_profile, get_next_available_slot) read anything out of it, and
only non-identity fields like a specialty name or doctor id.
"""
from app.clients import spring_client

_HANDLERS = {
    "get_policies": lambda arguments, user_id, role: spring_client.get_policies(),
    "get_appointment_count": lambda arguments, user_id, role: spring_client.get_appointment_count(user_id, role),
    "get_next_appointment": lambda arguments, user_id, role: spring_client.get_next_appointment(user_id, role),
    "get_today_queue_count": lambda arguments, user_id, role: spring_client.get_today_queue_count(user_id, role),
    "get_reschedule_status": lambda arguments, user_id, role: spring_client.get_reschedule_status(user_id, role),
    "get_refund_status": lambda arguments, user_id, role: spring_client.get_refund_status(user_id, role),
    "get_prescription_status": lambda arguments, user_id, role: spring_client.get_prescription_status(user_id, role),
    "get_upcoming_appointments": lambda arguments, user_id, role: spring_client.get_upcoming_appointments(user_id, role),
    "get_appointment_history": lambda arguments, user_id, role: spring_client.get_appointment_history(user_id, role),
    "get_my_profile": lambda arguments, user_id, role: spring_client.get_my_profile(user_id, role),
    "get_my_stats": lambda arguments, user_id, role: spring_client.get_my_stats(user_id, role),
    "get_family_members": lambda arguments, user_id, role: spring_client.get_family_members(user_id, role),
    "get_medical_records": lambda arguments, user_id, role: spring_client.get_medical_records(user_id, role),
    "get_second_opinions": lambda arguments, user_id, role: spring_client.get_second_opinions(user_id, role),
    "get_prescriptions": lambda arguments, user_id, role: spring_client.get_prescriptions(user_id, role),
    "get_payment_history": lambda arguments, user_id, role: spring_client.get_payment_history(user_id, role),
    "get_weekly_schedule": lambda arguments, user_id, role: spring_client.get_weekly_schedule(user_id, role),
    "get_treated_patients": lambda arguments, user_id, role: spring_client.get_treated_patients(user_id, role),
    "get_doctor_dashboard": lambda arguments, user_id, role: spring_client.get_doctor_dashboard(user_id, role),
    "get_my_reviews": lambda arguments, user_id, role: spring_client.get_my_reviews(user_id, role),
    "get_earnings_summary": lambda arguments, user_id, role: spring_client.get_earnings_summary(user_id, role),
    "get_earnings_list": lambda arguments, user_id, role: spring_client.get_earnings_list(user_id, role),
    "get_payouts": lambda arguments, user_id, role: spring_client.get_payouts(user_id, role),
    "list_specialties": lambda arguments, user_id, role: spring_client.list_specialties(),
    "search_doctors": lambda arguments, user_id, role: spring_client.search_doctors(
        arguments.get("specialization"), arguments.get("search")
    ),
    "get_doctor_profile": lambda arguments, user_id, role: spring_client.get_doctor_profile(
        arguments.get("doctor_id")
    ),
    "get_next_available_slot": lambda arguments, user_id, role: spring_client.get_next_available_slot(
        arguments.get("specialization")
    ),
}


def dispatch(
    name: str,
    arguments: dict,
    *,
    user_id: str | None = None,
    role: str | None = None,
) -> dict:
    handler = _HANDLERS.get(name)
    if handler is None:
        return {"error": f"Unknown tool '{name}'"}
    try:
        return handler(arguments, user_id, role)
    except Exception as exc:
        # A tool failure should degrade to "I don't know", not a 500 - the
        # caller is a patient asking a question, not a service integration.
        return {"error": f"Could not fetch this right now: {exc}"}
