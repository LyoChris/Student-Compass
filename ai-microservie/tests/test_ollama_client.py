import httpx
import pytest

from app.core.ollama_client import OllamaClient, OllamaUnavailable


def _transport(handler):
    return httpx.MockTransport(handler)


@pytest.mark.asyncio
async def test_generate_json_parses_response():
    def handler(request):
        return httpx.Response(200, json={"response": '{"k": 1}'})

    c = OllamaClient("http://x", "llama3", 5.0, transport=_transport(handler))
    assert await c.generate_json("hi") == {"k": 1}


@pytest.mark.asyncio
async def test_connection_error_raises_unavailable():
    def handler(request):
        raise httpx.ConnectError("down")

    c = OllamaClient("http://x", "llama3", 5.0, transport=_transport(handler))
    with pytest.raises(OllamaUnavailable):
        await c.generate_json("hi")


@pytest.mark.asyncio
async def test_non_json_output_raises_unavailable():
    def handler(request):
        return httpx.Response(200, json={"response": "not json"})

    c = OllamaClient("http://x", "llama3", 5.0, transport=_transport(handler))
    with pytest.raises(OllamaUnavailable):
        await c.generate_json("hi")
