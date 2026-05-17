from app.core.config import Settings


def test_settings_have_expected_defaults():
    s = Settings()
    assert s.ollama_base_url == "http://localhost:11434"
    assert s.ollama_model == "llama3"
    assert s.ollama_timeout_seconds == 60.0
    assert s.ai_service_shared_secret  # non-empty default for local/dev


def test_env_overrides(monkeypatch):
    monkeypatch.setenv("OLLAMA_MODEL", "llama3:70b")
    monkeypatch.setenv("AI_SERVICE_SHARED_SECRET", "topsecret")
    s = Settings()
    assert s.ollama_model == "llama3:70b"
    assert s.ai_service_shared_secret == "topsecret"
