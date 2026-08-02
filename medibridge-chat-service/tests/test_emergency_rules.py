from app.guardrails.emergency_rules import EMERGENCY_MESSAGE, is_emergency


def test_flags_known_emergency_phrases():
    assert is_emergency("I have chest pain and can't breathe")


def test_is_case_insensitive():
    assert is_emergency("Severe Bleeding from a cut")


def test_matches_a_keyword_inside_a_longer_sentence():
    assert is_emergency("my grandmother is unconscious on the floor")


def test_does_not_flag_routine_symptoms():
    assert not is_emergency("I have a mild headache and want to book a doctor")


def test_message_tells_the_patient_to_seek_real_help():
    assert "emergency" in EMERGENCY_MESSAGE.lower()
