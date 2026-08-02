import app.tools.handlers as handlers


def test_unknown_tool_degrades_to_error_dict():
    result = handlers.dispatch("not_a_real_tool", {})
    assert result == {"error": "Unknown tool 'not_a_real_tool'"}


def test_successful_handler_result_passes_through(monkeypatch):
    monkeypatch.setitem(
        handlers._HANDLERS, "get_policies", lambda user_id, role: {"free_cancellation_hours": 24}
    )
    assert handlers.dispatch("get_policies", {}) == {"free_cancellation_hours": 24}


def test_handler_exception_degrades_to_error_dict_not_a_raise(monkeypatch):
    def boom(user_id, role):
        raise ConnectionError("Spring is down")

    monkeypatch.setitem(handlers._HANDLERS, "get_policies", boom)
    result = handlers.dispatch("get_policies", {})
    assert result == {"error": "Could not fetch this right now: Spring is down"}


def test_role_mismatch_degrades_to_error_dict_end_to_end():
    # Exercises the real spring_client.get_today_queue_count - it raises
    # before any network call, so this is safe to run without a backend.
    result = handlers.dispatch("get_today_queue_count", {}, user_id="7", role="PATIENT")
    assert result == {"error": "Could not fetch this right now: Only a doctor has a today's queue"}
