import httpx
import pytest

from app.clients import spring_client


class _FakeResponse:
    def __init__(self, status_code, payload=None):
        self.status_code = status_code
        self._payload = payload or {}

    def raise_for_status(self):
        if self.status_code >= 400:
            raise httpx.HTTPStatusError("error", request=None, response=self)

    def json(self):
        return self._payload


def test_missing_record_degrades_to_found_false(monkeypatch):
    monkeypatch.setattr(httpx, "get", lambda *a, **k: _FakeResponse(404))
    assert spring_client.get_next_appointment("7", "PATIENT") == {"found": False}


def test_existing_record_is_marked_found(monkeypatch):
    monkeypatch.setattr(httpx, "get", lambda *a, **k: _FakeResponse(200, {"with_name": "Dr. Rao"}))
    assert spring_client.get_next_appointment("7", "PATIENT") == {"found": True, "with_name": "Dr. Rao"}


def test_real_failure_still_raises_not_swallowed_as_missing(monkeypatch):
    monkeypatch.setattr(httpx, "get", lambda *a, **k: _FakeResponse(500))
    with pytest.raises(httpx.HTTPStatusError):
        spring_client.get_next_appointment("7", "PATIENT")


def test_doctor_cannot_request_patient_only_data():
    with pytest.raises(ValueError):
        spring_client.get_refund_status("D001", "DOCTOR")


def test_patient_cannot_request_doctor_only_data():
    with pytest.raises(ValueError):
        spring_client.get_today_queue_count("7", "PATIENT")


def test_missing_identity_is_rejected():
    with pytest.raises(ValueError):
        spring_client.get_appointment_count(None, None)
