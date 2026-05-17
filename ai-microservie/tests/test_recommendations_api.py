import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, insert
from sqlalchemy.pool import StaticPool

from app.core.db import (
    catalog_products,
    get_db,
    metadata,
    stores,
    student_profiles,
)
from app.core.ollama_client import OllamaUnavailable, get_ollama_client
from app.main import create_app
from tests.conftest import SECRET_HEADER


def _seeded_engine():
    e = create_engine(
        "sqlite+pysqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
        future=True,
    )
    metadata.create_all(e)
    with e.begin() as conn:
        conn.execute(insert(student_profiles).values(
            user_id="11111111-1111-1111-1111-111111111111",
            living_area="DORMITORY", eating_habit="COOKING",
            home_package_frequency="MONTHLY", monthly_budget=1500,
            dorm_id=None,
        ))
        conn.execute(insert(stores).values(
            id="s1", name="Acme", is_partner=True))
        conn.execute(insert(catalog_products).values(
            id="p1", store_id="s1", name="Rice", price=9.5,
            category="FOOD", unit="kg", image_url=None,
            source_url=None, status="PUBLISHED"))
    return e


class FakeOllama:
    def __init__(self, fail=False):
        self._fail = fail

    async def generate_json(self, prompt):
        if self._fail:
            raise OllamaUnavailable("down")
        return {"recommendations": [{"productId": "p1", "reason": "Staple"}]}


def _client(fail=False):
    app = create_app()
    engine = _seeded_engine()

    def _db_override():
        with engine.connect() as conn:
            yield conn

    app.dependency_overrides[get_db] = _db_override
    app.dependency_overrides[get_ollama_client] = lambda: FakeOllama(fail=fail)
    return TestClient(app)


def test_requires_secret():
    resp = _client().post(
        "/api/v1/recommendations",
        json={"userId": "11111111-1111-1111-1111-111111111111"},
    )
    assert resp.status_code == 401


def test_llm_recommendations_ok():
    resp = _client().post(
        "/api/v1/recommendations",
        headers=SECRET_HEADER,
        json={"userId": "11111111-1111-1111-1111-111111111111"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["source"] == "llm"
    assert body["recommendations"][0]["productId"] == "p1"


def test_unknown_user_404():
    resp = _client().post(
        "/api/v1/recommendations",
        headers=SECRET_HEADER,
        json={"userId": "00000000-0000-0000-0000-000000000000"},
    )
    assert resp.status_code == 404


def test_fallback_source_when_ollama_down():
    resp = _client(fail=True).post(
        "/api/v1/recommendations",
        headers=SECRET_HEADER,
        json={"userId": "11111111-1111-1111-1111-111111111111"},
    )
    assert resp.status_code == 200
    assert resp.json()["source"] == "fallback"
