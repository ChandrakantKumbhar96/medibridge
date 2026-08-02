import app.tools.handlers as handlers


def test_unknown_tool_degrades_to_error_dict():
    result = handlers.dispatch("not_a_real_tool", {})
    assert result == {"error": "Unknown tool 'not_a_real_tool'"}


def test_successful_handler_result_passes_through(monkeypatch):
    monkeypatch.setitem(handlers._HANDLERS, "get_policies", lambda: {"free_cancellation_hours": 24})
    assert handlers.dispatch("get_policies", {}) == {"free_cancellation_hours": 24}


def test_handler_exception_degrades_to_error_dict_not_a_raise(monkeypatch):
    def boom():
        raise ConnectionError("Spring is down")

    monkeypatch.setitem(handlers._HANDLERS, "get_policies", boom)
    result = handlers.dispatch("get_policies", {})
    assert result == {"error": "Could not fetch this right now: Spring is down"}
