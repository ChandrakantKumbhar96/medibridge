"""HTTP calls to the Spring backend for tool handlers that need real data
instead of FAQ text - see app/tools/.

get_policies calls a permitAll endpoint, the same trust level as a browser.
Patient/doctor-scoped calls go through /internal/* instead, guarded by the
shared X-Internal-Api-Key (see InternalAppointmentController) rather than
the caller's JWT, which this service never sees - user_id/role here come
from the gateway forwarding its own verified token, not from the model.
"""
import httpx

from app.config import settings

TIMEOUT_SECONDS = 5.0
_INTERNAL_HEADERS = {"X-Internal-Api-Key": settings.spring_internal_api_key or ""}


def get_policies() -> dict:
    response = httpx.get(
        f"{settings.spring_api_url}/policies",
        timeout=TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    return response.json()


def _get_internal(path: str, params: dict | None = None) -> dict | int:
    response = httpx.get(
        f"{settings.spring_api_url}{path}",
        headers=_INTERNAL_HEADERS,
        params=params,
        timeout=TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    return response.json()


def _get_internal_or_missing(path: str, params: dict | None = None) -> dict:
    """Like _get_internal, but a 404 means the record genuinely doesn't
    exist (no upcoming appointment, no refund, no prescription yet, no
    matching slot) rather than a failure - {"found": False} lets the model
    say so plainly instead of degrading to "could not fetch this right now".
    """
    response = httpx.get(
        f"{settings.spring_api_url}{path}",
        headers=_INTERNAL_HEADERS,
        params=params,
        timeout=TIMEOUT_SECONDS,
    )
    if response.status_code == 404:
        return {"found": False}
    response.raise_for_status()
    return {"found": True, **response.json()}


def get_appointment_count(user_id: str | None, role: str | None) -> dict:
    if role == "PATIENT" and user_id:
        path = f"/internal/appointments/patient/{user_id}/count"
    elif role == "DOCTOR" and user_id:
        path = f"/internal/appointments/doctor/{user_id}/count"
    else:
        raise ValueError("No patient or doctor identity on this request")
    return {"upcoming_appointment_count": _get_internal(path)}


def get_next_appointment(user_id: str | None, role: str | None) -> dict:
    if role == "PATIENT" and user_id:
        path = f"/internal/appointments/patient/{user_id}/next"
    elif role == "DOCTOR" and user_id:
        path = f"/internal/appointments/doctor/{user_id}/next"
    else:
        raise ValueError("No patient or doctor identity on this request")
    return _get_internal_or_missing(path)


def get_today_queue_count(user_id: str | None, role: str | None) -> dict:
    if role != "DOCTOR" or not user_id:
        raise ValueError("Only a doctor has a today's queue")
    return {"today_appointment_count": _get_internal(f"/internal/appointments/doctor/{user_id}/today-count")}


def get_reschedule_status(user_id: str | None, role: str | None) -> dict:
    if role != "PATIENT" or not user_id:
        raise ValueError("Only a patient has a reschedule status")
    return _get_internal_or_missing(f"/internal/appointments/patient/{user_id}/reschedule-status")


def get_refund_status(user_id: str | None, role: str | None) -> dict:
    if role != "PATIENT" or not user_id:
        raise ValueError("Only a patient has a refund status")
    return _get_internal_or_missing(f"/internal/payments/patient/{user_id}/latest-refund")


def get_prescription_status(user_id: str | None, role: str | None) -> dict:
    if role != "PATIENT" or not user_id:
        raise ValueError("Only a patient has prescriptions")
    return _get_internal_or_missing(f"/internal/prescriptions/patient/{user_id}/latest")


def get_upcoming_appointments(user_id: str | None, role: str | None) -> dict:
    if role != "PATIENT" or not user_id:
        raise ValueError("Only a patient has upcoming appointments")
    return {"upcoming_appointments": _get_internal(f"/internal/appointments/patient/{user_id}/upcoming")}


def get_appointment_history(user_id: str | None, role: str | None) -> dict:
    if role != "PATIENT" or not user_id:
        raise ValueError("Only a patient has appointment history")
    return {"past_appointments": _get_internal(f"/internal/appointments/patient/{user_id}/history")}


def get_my_profile(user_id: str | None, role: str | None) -> dict:
    if role == "PATIENT" and user_id:
        return _get_internal(f"/internal/patients/{user_id}/profile")
    if role == "DOCTOR" and user_id:
        return _get_internal(f"/internal/doctors/{user_id}/profile")
    raise ValueError("No patient or doctor identity on this request")


def get_my_stats(user_id: str | None, role: str | None) -> dict:
    if role != "PATIENT" or not user_id:
        raise ValueError("Only a patient has patient stats")
    return _get_internal(f"/internal/patients/{user_id}/stats")


def get_family_members(user_id: str | None, role: str | None) -> dict:
    if role != "PATIENT" or not user_id:
        raise ValueError("Only a patient has family members")
    return {"family_members": _get_internal(f"/internal/patients/{user_id}/family-members")}


def get_medical_records(user_id: str | None, role: str | None) -> dict:
    if role != "PATIENT" or not user_id:
        raise ValueError("Only a patient has medical records")
    return {"medical_records": _get_internal(f"/internal/records/patient/{user_id}")}


def get_second_opinions(user_id: str | None, role: str | None) -> dict:
    if role != "PATIENT" or not user_id:
        raise ValueError("Only a patient has second opinion requests")
    return {"second_opinions": _get_internal(f"/internal/opinions/patient/{user_id}")}


def get_prescriptions(user_id: str | None, role: str | None) -> dict:
    if role != "PATIENT" or not user_id:
        raise ValueError("Only a patient has prescriptions")
    return {"prescriptions": _get_internal(f"/internal/prescriptions/patient/{user_id}")}


def get_payment_history(user_id: str | None, role: str | None) -> dict:
    if role != "PATIENT" or not user_id:
        raise ValueError("Only a patient has a payment history")
    return {"payments": _get_internal(f"/internal/payments/patient/{user_id}")}


def get_weekly_schedule(user_id: str | None, role: str | None) -> dict:
    if role != "DOCTOR" or not user_id:
        raise ValueError("Only a doctor has a weekly schedule")
    return {"weekly_schedule": _get_internal(f"/internal/doctors/{user_id}/weekly-schedule")}


def get_treated_patients(user_id: str | None, role: str | None) -> dict:
    if role != "DOCTOR" or not user_id:
        raise ValueError("Only a doctor has treated patients")
    return {"treated_patients": _get_internal(f"/internal/doctors/{user_id}/treated-patients")}


def get_doctor_dashboard(user_id: str | None, role: str | None) -> dict:
    if role != "DOCTOR" or not user_id:
        raise ValueError("Only a doctor has a dashboard")
    return _get_internal(f"/internal/appointments/doctor/{user_id}/dashboard")


def get_my_reviews(user_id: str | None, role: str | None) -> dict:
    if role != "DOCTOR" or not user_id:
        raise ValueError("Only a doctor has reviews")
    return {"reviews": _get_internal(f"/internal/reviews/doctor/{user_id}")}


def get_earnings_summary(user_id: str | None, role: str | None) -> dict:
    if role != "DOCTOR" or not user_id:
        raise ValueError("Only a doctor has earnings")
    return _get_internal(f"/internal/payouts/doctor/{user_id}/summary")


def get_earnings_list(user_id: str | None, role: str | None) -> dict:
    if role != "DOCTOR" or not user_id:
        raise ValueError("Only a doctor has earnings")
    return {"earnings": _get_internal(f"/internal/payouts/doctor/{user_id}/earnings")}


def get_payouts(user_id: str | None, role: str | None) -> dict:
    if role != "DOCTOR" or not user_id:
        raise ValueError("Only a doctor has payouts")
    return {"payouts": _get_internal(f"/internal/payouts/doctor/{user_id}")}


# ---------------------------------------------------------------- discovery
# Public browsing data - not scoped to a caller's identity, so these take no
# user_id/role. list_specialties hits Spring's permitAll /specialties
# directly, the same trust level as a browser; the rest go through
# /internal/* like everything else because /doctors and /next-available
# require a logged-in caller on the Spring side, not anonymous browsing.


def list_specialties() -> dict:
    response = httpx.get(f"{settings.spring_api_url}/specialties", timeout=TIMEOUT_SECONDS)
    response.raise_for_status()
    return {"specialties": response.json()}


def search_doctors(specialization: str | None, search: str | None) -> dict:
    params = {k: v for k, v in {"specialization": specialization, "search": search}.items() if v}
    return {"doctors": _get_internal("/internal/doctors/search", params=params)}


def get_doctor_profile(doctor_id: str | None) -> dict:
    if not doctor_id:
        raise ValueError("doctor_id is required")
    return _get_internal_or_missing(f"/internal/doctors/{doctor_id}/public-profile")


def get_next_available_slot(specialization: str | None) -> dict:
    params = {"specialization": specialization} if specialization else None
    return _get_internal_or_missing("/internal/appointments/next-available", params=params)
