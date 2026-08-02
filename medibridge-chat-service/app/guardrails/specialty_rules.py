import json
from pathlib import Path

MAPPING_PATH = Path(__file__).resolve().parent.parent / "knowledge" / "specialty_mapping.json"
_RULES = json.loads(MAPPING_PATH.read_text(encoding="utf-8"))["rules"]


def match_specialty(text: str) -> dict | None:
    lowered = text.lower()
    for rule in _RULES:
        if any(keyword in lowered for keyword in rule["keywords"]):
            return {"specialty": rule["specialty"], "urgency": rule["urgency"]}
    return None
