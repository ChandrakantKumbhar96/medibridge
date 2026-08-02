from app.guardrails.specialty_rules import match_specialty


def test_matches_the_rule_whose_keyword_appears():
    result = match_specialty("I've had shortness of breath since this morning")
    assert result == {"specialty": "Cardiology", "urgency": "urgent"}


def test_is_case_insensitive():
    result = match_specialty("Bad MIGRAINE for two days")
    assert result == {"specialty": "Neurology", "urgency": "routine"}


def test_returns_none_when_nothing_matches():
    assert match_specialty("I'd like to book a routine check-up") is None
