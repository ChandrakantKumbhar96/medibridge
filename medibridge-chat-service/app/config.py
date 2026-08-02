from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    port: int = 8000
    env: str = "development"
    cors_origin: str = "http://localhost:4000"

    chroma_persist_dir: str = "./data/chroma"
    embedding_model: str = "sentence-transformers/all-MiniLM-L6-v2"

    groq_api_key: str | None = None
    groq_model: str = "llama-3.3-70b-versatile"
    groq_model_fast: str = "llama-3.1-8b-instant"

    redis_url: str = "redis://localhost:6379/0"

    # Shared secret only the gateway knows - see app/auth/internal_key.py.
    internal_api_key: str | None = None

    # Base URL of the Spring backend (context path included), for tool calls
    # that need real data instead of FAQ text - see app/clients/spring_client.py.
    # Called directly, not through the gateway: these are all permitAll GETs,
    # the same trust level as a browser hitting them.
    spring_api_url: str = "http://localhost:8080/api"


settings = Settings()
