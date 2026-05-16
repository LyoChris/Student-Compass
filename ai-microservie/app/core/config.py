from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_name: str = "Student Compass AI"

    # Private-service guard (override in real envs).
    ai_service_shared_secret: str = "dev-internal-secret"

    # Read-only shared DB. Defaults to sqlite for local/test.
    database_url: str = "sqlite+pysqlite:///:memory:"

    # In-container Ollama.
    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "llama3"
    ollama_timeout_seconds: float = 60.0


settings = Settings()
