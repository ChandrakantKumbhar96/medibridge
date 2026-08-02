# This service is otherwise stateless, so multi-turn triage conversation
# state lives here, keyed by session_id, with a TTL.
import json
from functools import lru_cache

import redis

from app.config import settings

SESSION_TTL_SECONDS = 1800


@lru_cache
def get_client() -> redis.Redis:
    return redis.from_url(settings.redis_url, decode_responses=True)


def _key(session_id: str) -> str:
    return f"triage:session:{session_id}"


def load_history(session_id: str) -> list[dict]:
    raw = get_client().get(_key(session_id))
    return json.loads(raw) if raw else []


def save_history(session_id: str, history: list[dict]) -> None:
    get_client().set(_key(session_id), json.dumps(history), ex=SESSION_TTL_SECONDS)


def clear(session_id: str) -> None:
    get_client().delete(_key(session_id))
