import fitz
from fastapi.testclient import TestClient

from app.core.ollama_client import OllamaUnavailable, get_ollama_client
from app.main import create_app
from tests.conftest import SECRET_HEADER

RAW_LINE = "2026-03-02 SECRETMERCHANT -333.33"


def _pdf(text: str) -> bytes:
    doc = fitz.open()
    page = doc.new_page()
    page.insert_text((72, 72), text)
    data = doc.tobytes()
    doc.close()
    return data


def _pdf_no_text() -> bytes:
    doc = fitz.open()
    doc.new_page()
    data = doc.tobytes()
    doc.close()
    return data


class FakeOllama:
    def __init__(self, fail=False):
        self._fail = fail

    async def generate_json(self, prompt):
        if self._fail:
            raise OllamaUnavailable("down")
        return {
            "periodLabel": "Mar 2026",
            "totalSpent": 333.33,
            "categories": [{"category": "OTHER", "amount": 333.33}],
            "questions": [],
        }


def _client(fail=False):
    app = create_app()
    app.dependency_overrides[get_ollama_client] = lambda: FakeOllama(fail=fail)
    return TestClient(app)


def test_requires_secret():
    resp = _client().post(
        "/api/v1/bank/analyze",
        files={"file": ("s.pdf", _pdf(RAW_LINE), "application/pdf")},
    )
    assert resp.status_code == 401


def test_analyze_returns_insights_only_no_raw_leak():
    resp = _client().post(
        "/api/v1/bank/analyze",
        headers=SECRET_HEADER,
        files={"file": ("s.pdf", _pdf(RAW_LINE), "application/pdf")},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["insights"]["totalSpent"] == 333.33
    assert "SECRETMERCHANT" not in resp.text  # no raw line leak


def test_empty_text_layer_422():
    resp = _client().post(
        "/api/v1/bank/analyze",
        headers=SECRET_HEADER,
        files={"file": ("s.pdf", _pdf_no_text(), "application/pdf")},
    )
    assert resp.status_code == 422


def test_ollama_down_503():
    resp = _client(fail=True).post(
        "/api/v1/bank/analyze",
        headers=SECRET_HEADER,
        files={"file": ("s.pdf", _pdf(RAW_LINE), "application/pdf")},
    )
    assert resp.status_code == 503
