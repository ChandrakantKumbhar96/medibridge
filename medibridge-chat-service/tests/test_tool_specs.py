from app.tools.handlers import _HANDLERS
from app.tools.specs import tools_for


def _names(tools):
    return {t["function"]["name"] for t in tools}


def test_patient_gets_patient_only_tools_not_doctor_only():
    names = _names(tools_for("PATIENT"))
    assert {"get_reschedule_status", "get_refund_status", "get_prescription_status"} <= names
    assert "get_today_queue_count" not in names


def test_doctor_gets_doctor_only_tools_not_patient_only():
    names = _names(tools_for("DOCTOR"))
    assert "get_today_queue_count" in names
    assert not {"get_reschedule_status", "get_refund_status", "get_prescription_status"} & names


def test_unknown_or_missing_role_gets_policies_only():
    assert _names(tools_for(None)) == {"get_policies"}
    assert _names(tools_for("ADMIN")) == {"get_policies"}


def test_every_advertised_tool_has_a_registered_handler():
    advertised = _names(tools_for("PATIENT")) | _names(tools_for("DOCTOR"))
    assert advertised <= set(_HANDLERS)
