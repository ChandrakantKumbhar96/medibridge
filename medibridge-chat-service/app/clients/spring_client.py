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
_INTERNAL_HEADERS = {"X-Internal-Api-Key": settings.internal_api_key or ""}


def get_policies() -> dict:
    response = httpx.get(
        f"{settings.spring_api_url}/policies",
        timeout=TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    return response.json()


def _get_internal(path: str) -> dict | int:
    response = httpx.get(
        f"{settings.spring_api_url}{path}",
        headers=_INTERNAL_HEADERS,
        timeout=TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    return response.json()


def _get_internal_or_missing(path: str) -> dict:
    """Like _get_internal, but a 404 means the record genuinely doesn't
    exist (no upcoming appointment, no refund, no prescription yet) rather
    than a failure - {"found": False} lets the model say so plainly instead
    of degrading to "could not fetch this right now".
    """
    response = httpx.get(
        f"{settings.spring_api_url}{path}",
        headers=_INTERNAL_HEADERS,
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
