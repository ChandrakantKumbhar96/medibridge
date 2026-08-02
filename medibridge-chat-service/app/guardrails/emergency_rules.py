# Hard keyword override: these bypass the LLM entirely and return a fixed
# emergency message. The LLM must never be the one to decide "this isn't
# an emergency" - that decision has to be made before it's ever called.
EMERGENCY_KEYWORDS = [
    "chest pain",
    "can't breathe",
    "cannot breathe",
    "difficulty breathing",
    "stroke",
    "face drooping",
    "slurred speech",
    "unconscious",
    "severe bleeding",
    "suicidal",
]

EMERGENCY_MESSAGE = (
    "This may be a medical emergency. Please call your local emergency "
    "number or go to the nearest emergency room immediately. This chatbot "
    "cannot help with emergencies."
)


def is_emergency(message: str) -> bool:
    lowered = message.lower()
    return any(keyword in lowered for keyword in EMERGENCY_KEYWORDS)
