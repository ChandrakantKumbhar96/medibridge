# Wired as a router-level dependency on both /chat and /triage. Only the
# gateway should be able to reach this service; it proves that with a shared
# key, not a user JWT - this service never sees end-user tokens.
from fastapi import Header, HTTPException

from app.config import settings


def require_internal_key(x_internal_api_key: str | None = Header(default=None)) -> None:
    if not settings.internal_api_key or x_internal_api_key != settings.internal_api_key:
        raise HTTPException(status_code=401, detail="Missing or invalid internal API key")
