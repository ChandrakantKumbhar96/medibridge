"""HTTP calls to the Spring backend for tool handlers that need real data
instead of FAQ text - see app/tools/.

Only permitAll endpoints are called here (GET /policies, the same trust
level as a browser). Nothing patient- or doctor-scoped goes through this
client: that would need the caller's JWT, which this service never sees.
"""
import httpx

from app.config import settings

TIMEOUT_SECONDS = 5.0


def get_policies() -> dict:
    response = httpx.get(
        f"{settings.spring_api_url}/policies",
        timeout=TIMEOUT_SECONDS,
    )
    response.raise_for_status()
    return response.json()
