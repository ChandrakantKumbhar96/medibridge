from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes_chat import router as chat_router
from app.api.routes_triage import router as triage_router
from app.config import settings

app = FastAPI(title="medibridge-chat-service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[settings.cors_origin],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat_router)
app.include_router(triage_router)


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "service": "medibridge-chat-service"}
