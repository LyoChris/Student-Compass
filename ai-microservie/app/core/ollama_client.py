import json

import httpx

from app.core.config import settings


class OllamaUnavailable(RuntimeError):
    """Ollama is unreachable, timed out, or returned non-JSON output."""


class OllamaClient:
    def __init__(
        self,
        base_url: str,
        model: str,
        timeout: float,
        transport: httpx.BaseTransport | None = None,
    ) -> None:
        self._base_url = base_url.rstrip("/")
        self._model = model
        self._timeout = timeout
        self._transport = transport

    async def generate_json(self, prompt: str) -> dict:
        url = f"{self._base_url}/api/generate"
        payload = {
            "model": self._model,
            "prompt": prompt,
            "stream": False,
            "format": "json",
        }
        try:
            async with httpx.AsyncClient(
                timeout=self._timeout, transport=self._transport
            ) as client:
                resp = await client.post(url, json=payload)
                resp.raise_for_status()
        except httpx.HTTPError as exc:
            raise OllamaUnavailable(str(exc)) from exc

        try:
            body = resp.json()
            raw = body.get("response", "") if isinstance(body, dict) else ""
            parsed = json.loads(raw)
        except (json.JSONDecodeError, TypeError) as exc:
            raise OllamaUnavailable(f"non-JSON model output: {exc}") from exc
        if not isinstance(parsed, dict):
            raise OllamaUnavailable("model output was not a JSON object")
        return parsed


def get_ollama_client() -> OllamaClient:
    return OllamaClient(
        settings.ollama_base_url,
        settings.ollama_model,
        settings.ollama_timeout_seconds,
    )
