from types import SimpleNamespace

from app.llm import groq_client


class _FakeToolCall:
    def __init__(self, call_id, name, arguments="{}"):
        self.id = call_id
        self.function = SimpleNamespace(name=name, arguments=arguments)


def _message(content=None, tool_calls=None):
    return SimpleNamespace(content=content, tool_calls=tool_calls or [])


def _response(message):
    return SimpleNamespace(choices=[SimpleNamespace(message=message)])


def _fake_client(create_fn):
    class FakeCompletions:
        def create(self, **kwargs):
            return create_fn(**kwargs)

    return SimpleNamespace(chat=SimpleNamespace(completions=FakeCompletions()))


def test_stops_after_one_round_if_the_model_never_calls_a_tool(monkeypatch):
    calls = []

    def create_fn(**kwargs):
        calls.append(kwargs)
        return _response(_message(content="the answer"))

    monkeypatch.setattr(groq_client, "get_client", lambda: _fake_client(create_fn))

    result = groq_client.complete_with_tools("system", "question", [], lambda name, args: {})

    assert result == "the answer"
    assert len(calls) == 1


def test_dispatches_a_tool_call_then_returns_the_follow_up_answer(monkeypatch):
    responses = [
        _response(_message(tool_calls=[_FakeToolCall("call_1", "get_policies")])),
        _response(_message(content="here is the policy")),
    ]
    monkeypatch.setattr(groq_client, "get_client", lambda: _fake_client(lambda **kw: responses.pop(0)))

    dispatched = []

    def dispatch(name, arguments):
        dispatched.append(name)
        return {"free_cancellation_hours": 24}

    result = groq_client.complete_with_tools("system", "question", [], dispatch)

    assert result == "here is the policy"
    assert dispatched == ["get_policies"]


def test_gives_up_after_max_rounds_and_forces_a_tool_less_final_answer(monkeypatch):
    call_count = {"n": 0}

    def create_fn(**kwargs):
        call_count["n"] += 1
        if "tools" in kwargs:
            return _response(_message(tool_calls=[_FakeToolCall(f"call_{call_count['n']}", "get_policies")]))
        return _response(_message(content="final answer"))

    monkeypatch.setattr(groq_client, "get_client", lambda: _fake_client(create_fn))

    result = groq_client.complete_with_tools("system", "question", [], lambda name, args: {"x": 1})

    assert result == "final answer"
    # MAX_TOOL_ROUNDS calls that kept requesting tools, plus one forced final call.
    assert call_count["n"] == groq_client.MAX_TOOL_ROUNDS + 1
