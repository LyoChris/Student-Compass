# AI Microservice (Recommendations + Bank) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an Ollama-powered `recommendations` endpoint and a privacy-first `bank/analyze` endpoint to the stateless FastAPI `ai/` service, with a shared-secret guard and read-only shared-DB access.

**Architecture:** FastAPI app-factory unchanged. `/health` stays public; `/api/v1/*` is guarded by an `X-Internal-Secret` header. Recommendations reads the user's profile + published catalog products from the shared Postgres (read-only) and asks local Ollama `llama3` to rank them, with a deterministic fallback. Bank parses an uploaded PDF's text layer in-memory with PyMuPDF, asks Ollama to derive insights, and returns insights only (no raw lines, ever). Ollama (`llama3`) is baked into the container image.

**Tech Stack:** Python 3.11, FastAPI, pydantic v2 / pydantic-settings, SQLAlchemy 2 Core (read-only), httpx (Ollama), PyMuPDF, pytest + pytest-asyncio. No Anthropic. No scraper.

**Working dir:** `ai/ai-microservie/` (run `pytest` here). Git repo root `ai/` (own `.git`; current branch `feat/ai-recommendations-and-bank`; commit with `git -C ai`).

**Spec:** `ai/docs/superpowers/specs/2026-05-16-ai-microservice-design.md`. Conventions: `.agents/rules/conventions.md`.

---

## File structure

| File | Responsibility | Action |
|------|----------------|--------|
| `ai/ai-microservie/requirements.txt` | pinned deps | Modify |
| `ai/ai-microservie/pytest.ini` | pytest config | Create |
| `ai/ai-microservie/tests/__init__.py` | tests package | Create |
| `ai/ai-microservie/tests/conftest.py` | app/client/db/ollama fixtures | Create |
| `ai/ai-microservie/app/core/config.py` | env settings | Modify |
| `ai/ai-microservie/app/core/security.py` | shared-secret guard | Create |
| `ai/ai-microservie/app/core/db.py` | read-only SQLAlchemy Core | Create |
| `ai/ai-microservie/app/core/ollama_client.py` | Ollama httpx wrapper | Create |
| `ai/ai-microservie/app/main.py` | mount routers | Modify |
| `ai/ai-microservie/app/modules/recommendations/__init__.py` | package | Create |
| `ai/ai-microservie/app/modules/recommendations/schemas.py` | pydantic models | Create |
| `ai/ai-microservie/app/modules/recommendations/repository.py` | read-only SELECTs | Create |
| `ai/ai-microservie/app/modules/recommendations/service.py` | prompt + rank + fallback | Create |
| `ai/ai-microservie/app/api/v1/recommendations.py` | router | Create |
| `ai/ai-microservie/app/modules/bank/__init__.py` | package | Create |
| `ai/ai-microservie/app/modules/bank/schemas.py` | pydantic models | Create |
| `ai/ai-microservie/app/modules/bank/pdf.py` | in-memory PDF text | Create |
| `ai/ai-microservie/app/modules/bank/service.py` | Ollama structuring | Create |
| `ai/ai-microservie/app/api/v1/bank.py` | router | Create |
| `ai/ai-microservie/Dockerfile` | Ollama + llama3 baked | Modify |
| `ai/ai-microservie/docker-compose.yml` | env wiring | Modify |
| `ai/ai-microservie/entrypoint.sh` | start ollama + uvicorn | Create |
| `ai/docs/backend-integration-suggestions.md` | backend doc (not applied) | Create |
| `ai/docs/decisions-override.md` | §3 override recorded inside ai/ | Create |

---

# Phase 0 — Plumbing: deps, config, security guard

## Task 0.1: Dependencies + pytest config

**Files:**
- Modify: `ai/ai-microservie/requirements.txt`
- Create: `ai/ai-microservie/pytest.ini`
- Create: `ai/ai-microservie/tests/__init__.py`

- [ ] **Step 1: Replace `requirements.txt`**

```
fastapi==0.111.0
uvicorn[standard]==0.30.1
pydantic==2.7.4
pydantic-settings==2.3.4
httpx==0.27.2
pymupdf==1.24.10
sqlalchemy==2.0.35
psycopg2-binary==2.9.9
pytest==8.3.3
pytest-asyncio==0.24.0
```

- [ ] **Step 2: Create `tests/__init__.py`** (empty file)

- [ ] **Step 3: Create `pytest.ini`**

```ini
[pytest]
testpaths = tests
asyncio_mode = auto
```

- [ ] **Step 4: Install deps**

Run: `cd ai/ai-microservie && pip install -r requirements.txt`
Expected: all install without error.

- [ ] **Step 5: Commit**

```bash
git -C ai add ai-microservie/requirements.txt ai-microservie/pytest.ini ai-microservie/tests/__init__.py
git -C ai commit -m "chore(ai): add deps + pytest config for recommendations/bank"
```

---

## Task 0.2: Env-driven config

**Files:**
- Modify: `ai/ai-microservie/app/core/config.py`
- Test: `ai/ai-microservie/tests/test_config.py`

- [ ] **Step 1: Write the failing test**

Create `ai/ai-microservie/tests/test_config.py`:

```python
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ai/ai-microservie && pytest tests/test_config.py -v`
Expected: FAIL — `Settings` has no `ollama_base_url`.

- [ ] **Step 3: Replace `app/core/config.py`**

```python
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ai/ai-microservie && pytest tests/test_config.py -v`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git -C ai add ai-microservie/app/core/config.py ai-microservie/tests/test_config.py
git -C ai commit -m "feat(ai): env-driven settings (secret, db, ollama)"
```

---

## Task 0.3: Shared-secret guard + conftest

**Files:**
- Create: `ai/ai-microservie/app/core/security.py`
- Create: `ai/ai-microservie/tests/conftest.py`
- Test: `ai/ai-microservie/tests/test_security.py`

- [ ] **Step 1: Create `tests/conftest.py`**

```python
import pytest
from fastapi.testclient import TestClient

from app.main import create_app


@pytest.fixture
def app():
    return create_app()


@pytest.fixture
def client(app):
    return TestClient(app)


SECRET_HEADER = {"X-Internal-Secret": "dev-internal-secret"}
```

- [ ] **Step 2: Write the failing test**

Create `ai/ai-microservie/tests/test_security.py`:

```python
from tests.conftest import SECRET_HEADER


def test_health_is_public(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}


def test_guarded_route_requires_secret(client):
    # /api/v1/_guard_probe is a guard-only probe mounted for tests below.
    resp = client.get("/api/v1/_guard_probe")
    assert resp.status_code == 401


def test_guarded_route_accepts_correct_secret(client):
    resp = client.get("/api/v1/_guard_probe", headers=SECRET_HEADER)
    assert resp.status_code == 200
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd ai/ai-microservie && pytest tests/test_security.py -v`
Expected: FAIL — no `security` module / `/api/v1/_guard_probe` route.

- [ ] **Step 4: Create `app/core/security.py`**

```python
import secrets

from fastapi import Header, HTTPException, status

from app.core.config import settings


async def require_internal_secret(
    x_internal_secret: str | None = Header(default=None),
) -> None:
    """Reject any request that does not present the shared internal secret.

    Applied to every /api/v1 router. /health stays public.
    """
    expected = settings.ai_service_shared_secret
    if not x_internal_secret or not secrets.compare_digest(
        x_internal_secret, expected
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing internal secret",
        )
```

- [ ] **Step 5: Add a guard probe route + mount it (temporary, kept)**

Replace `ai/ai-microservie/app/main.py` entirely:

```python
from fastapi import APIRouter, Depends, FastAPI

from app.api.v1.health import router as health_router
from app.core.security import require_internal_secret

_guard_probe = APIRouter(
    prefix="/api/v1", dependencies=[Depends(require_internal_secret)]
)


@_guard_probe.get("/_guard_probe")
def _guard_probe_route() -> dict:
    return {"ok": True}


def create_app() -> FastAPI:
    app = FastAPI(title="Student Compass AI", version="0.1.0")
    app.include_router(health_router)
    app.include_router(_guard_probe)
    return app


app = create_app()
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd ai/ai-microservie && pytest tests/test_security.py -v`
Expected: PASS (3 tests).

- [ ] **Step 7: Commit**

```bash
git -C ai add ai-microservie/app/core/security.py ai-microservie/app/main.py \
  ai-microservie/tests/conftest.py ai-microservie/tests/test_security.py
git -C ai commit -m "feat(ai): X-Internal-Secret guard on /api/v1 (health stays public)"
```

---

# Phase 1 — Shared infra: DB read layer + Ollama client

## Task 1.1: Read-only SQLAlchemy Core layer

**Files:**
- Create: `ai/ai-microservie/app/core/db.py`
- Test: `ai/ai-microservie/tests/test_db.py`

- [ ] **Step 1: Write the failing test**

Create `ai/ai-microservie/tests/test_db.py`:

```python
from sqlalchemy import create_engine, insert
from sqlalchemy.pool import StaticPool

from app.core.db import metadata, catalog_products, stores


def test_read_models_create_and_select():
    engine = create_engine(
        "sqlite+pysqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
        future=True,
    )
    metadata.create_all(engine)
    with engine.begin() as conn:
        conn.execute(insert(stores).values(id="s1", name="Acme", is_partner=True))
        conn.execute(
            insert(catalog_products).values(
                id="p1", store_id="s1", name="Rice 1kg", price=9.5,
                category="FOOD", unit="kg", image_url=None,
                source_url=None, status="PUBLISHED",
            )
        )
    with engine.connect() as conn:
        rows = conn.execute(catalog_products.select()).fetchall()
    assert rows[0].name == "Rice 1kg"
    assert rows[0].status == "PUBLISHED"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ai/ai-microservie && pytest tests/test_db.py -v`
Expected: FAIL — `app.core.db` does not exist.

- [ ] **Step 3: Create `app/core/db.py`**

```python
from collections.abc import Iterator

from sqlalchemy import (
    Boolean,
    Column,
    MetaData,
    Numeric,
    String,
    Table,
    create_engine,
)
from sqlalchemy.engine import Connection, Engine

from app.core.config import settings

metadata = MetaData()

# Lightweight read models — ONLY the columns the AI service SELECTs.
# The backend owns these tables and their Flyway migrations.

student_profiles = Table(
    "student_profiles",
    metadata,
    Column("user_id", String, primary_key=True),
    Column("living_area", String),
    Column("eating_habit", String),
    Column("home_package_frequency", String),
    Column("monthly_budget", Numeric),
    Column("dorm_id", String),
)

student_fixed_expenses = Table(
    "student_fixed_expenses",
    metadata,
    Column("id", String, primary_key=True),
    Column("profile_id", String),
    Column("expense_name", String),
    Column("amount", Numeric),
)

stores = Table(
    "stores",
    metadata,
    Column("id", String, primary_key=True),
    Column("name", String),
    Column("is_partner", Boolean),
)

catalog_products = Table(
    "catalog_products",
    metadata,
    Column("id", String, primary_key=True),
    Column("store_id", String),
    Column("name", String),
    Column("price", Numeric),
    Column("category", String),
    Column("unit", String),
    Column("image_url", String),
    Column("source_url", String),
    Column("status", String),
)

_engine: Engine | None = None


def get_engine() -> Engine:
    global _engine
    if _engine is None:
        _engine = create_engine(settings.database_url, future=True)
    return _engine


def get_db() -> Iterator[Connection]:
    """FastAPI dependency: a read-only connection per request."""
    with get_engine().connect() as conn:
        yield conn
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ai/ai-microservie && pytest tests/test_db.py -v`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git -C ai add ai-microservie/app/core/db.py ai-microservie/tests/test_db.py
git -C ai commit -m "feat(ai): read-only SQLAlchemy Core read models"
```

---

## Task 1.2: Ollama client

**Files:**
- Create: `ai/ai-microservie/app/core/ollama_client.py`
- Test: `ai/ai-microservie/tests/test_ollama_client.py`

- [ ] **Step 1: Write the failing test**

Create `ai/ai-microservie/tests/test_ollama_client.py`:

```python
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ai/ai-microservie && pytest tests/test_ollama_client.py -v`
Expected: FAIL — `app.core.ollama_client` does not exist.

- [ ] **Step 3: Create `app/core/ollama_client.py`**

```python
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

        raw = resp.json().get("response", "")
        try:
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ai/ai-microservie && pytest tests/test_ollama_client.py -v`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git -C ai add ai-microservie/app/core/ollama_client.py ai-microservie/tests/test_ollama_client.py
git -C ai commit -m "feat(ai): Ollama httpx client with typed unavailability"
```

---

# Phase 2 — Recommendations module

## Task 2.1: Schemas + repository

**Files:**
- Create: `ai/ai-microservie/app/modules/recommendations/__init__.py` (empty)
- Create: `ai/ai-microservie/app/modules/recommendations/schemas.py`
- Create: `ai/ai-microservie/app/modules/recommendations/repository.py`
- Test: `ai/ai-microservie/tests/test_recommendations_repository.py`

- [ ] **Step 1: Write the failing test**

Create `ai/ai-microservie/tests/test_recommendations_repository.py`:

```python
from sqlalchemy import create_engine, insert
from sqlalchemy.pool import StaticPool

from app.core.db import (
    metadata,
    student_profiles,
    student_fixed_expenses,
    stores,
    catalog_products,
)
from app.modules.recommendations.repository import (
    load_profile,
    load_published_products,
)


def _engine():
    e = create_engine(
        "sqlite+pysqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
        future=True,
    )
    metadata.create_all(e)
    return e


def test_load_profile_returns_none_when_missing():
    e = _engine()
    with e.connect() as conn:
        assert load_profile(conn, "u-missing") is None


def test_load_profile_and_products():
    e = _engine()
    with e.begin() as conn:
        conn.execute(insert(student_profiles).values(
            user_id="u1", living_area="DORMITORY", eating_habit="COOKING",
            home_package_frequency="MONTHLY", monthly_budget=1500, dorm_id=None,
        ))
        conn.execute(insert(student_fixed_expenses).values(
            id="f1", profile_id="u1", expense_name="Gym", amount=80,
        ))
        conn.execute(insert(stores).values(id="s1", name="Acme", is_partner=True))
        conn.execute(insert(catalog_products).values(
            id="p1", store_id="s1", name="Rice", price=9.5, category="FOOD",
            unit="kg", image_url=None, source_url=None, status="PUBLISHED",
        ))
        conn.execute(insert(catalog_products).values(
            id="p2", store_id="s1", name="Draft item", price=1, category="FOOD",
            unit=None, image_url=None, source_url=None, status="DRAFT",
        ))
    with e.connect() as conn:
        prof = load_profile(conn, "u1")
        prods = load_published_products(conn)

    assert prof.living_area == "DORMITORY"
    assert prof.fixed_expenses == [("Gym", 80.0)]
    assert [p.product_id for p in prods] == ["p1"]      # DRAFT excluded
    assert prods[0].store_name == "Acme"
    assert prods[0].is_partner is True
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ai/ai-microservie && pytest tests/test_recommendations_repository.py -v`
Expected: FAIL — module does not exist.

- [ ] **Step 3: Create `app/modules/recommendations/__init__.py`** (empty file)

- [ ] **Step 4: Create `app/modules/recommendations/schemas.py`**

```python
from dataclasses import dataclass, field

from pydantic import BaseModel
from uuid import UUID


class RecommendationRequest(BaseModel):
    userId: UUID


class RecommendationItem(BaseModel):
    productId: str
    name: str
    price: float
    category: str
    storeName: str
    isPartner: bool
    reason: str


class RecommendationResponse(BaseModel):
    userId: str
    source: str  # "llm" | "fallback"
    recommendations: list[RecommendationItem]


@dataclass
class ProfileRow:
    user_id: str
    living_area: str | None
    eating_habit: str | None
    home_package_frequency: str | None
    monthly_budget: float | None
    dorm_id: str | None
    fixed_expenses: list[tuple[str, float]] = field(default_factory=list)


@dataclass
class ProductRow:
    product_id: str
    name: str
    price: float
    category: str
    store_name: str
    is_partner: bool
```

- [ ] **Step 5: Create `app/modules/recommendations/repository.py`**

```python
from sqlalchemy import select
from sqlalchemy.engine import Connection

from app.core.db import (
    catalog_products,
    stores,
    student_fixed_expenses,
    student_profiles,
)
from app.modules.recommendations.schemas import ProductRow, ProfileRow


def load_profile(conn: Connection, user_id: str) -> ProfileRow | None:
    row = conn.execute(
        select(
            student_profiles.c.user_id,
            student_profiles.c.living_area,
            student_profiles.c.eating_habit,
            student_profiles.c.home_package_frequency,
            student_profiles.c.monthly_budget,
            student_profiles.c.dorm_id,
        ).where(student_profiles.c.user_id == user_id)
    ).first()
    if row is None:
        return None

    expenses = conn.execute(
        select(
            student_fixed_expenses.c.expense_name,
            student_fixed_expenses.c.amount,
        ).where(student_fixed_expenses.c.profile_id == user_id)
    ).fetchall()

    return ProfileRow(
        user_id=str(row.user_id),
        living_area=row.living_area,
        eating_habit=row.eating_habit,
        home_package_frequency=row.home_package_frequency,
        monthly_budget=float(row.monthly_budget)
        if row.monthly_budget is not None
        else None,
        dorm_id=str(row.dorm_id) if row.dorm_id is not None else None,
        fixed_expenses=[(e.expense_name, float(e.amount)) for e in expenses],
    )


def load_published_products(conn: Connection) -> list[ProductRow]:
    rows = conn.execute(
        select(
            catalog_products.c.id,
            catalog_products.c.name,
            catalog_products.c.price,
            catalog_products.c.category,
            stores.c.name.label("store_name"),
            stores.c.is_partner,
        )
        .select_from(
            catalog_products.join(
                stores, catalog_products.c.store_id == stores.c.id
            )
        )
        .where(catalog_products.c.status == "PUBLISHED")
    ).fetchall()
    return [
        ProductRow(
            product_id=str(r.id),
            name=r.name,
            price=float(r.price),
            category=r.category,
            store_name=r.store_name,
            is_partner=bool(r.is_partner),
        )
        for r in rows
    ]
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd ai/ai-microservie && pytest tests/test_recommendations_repository.py -v`
Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git -C ai add ai-microservie/app/modules/recommendations/__init__.py \
  ai-microservie/app/modules/recommendations/schemas.py \
  ai-microservie/app/modules/recommendations/repository.py \
  ai-microservie/tests/test_recommendations_repository.py
git -C ai commit -m "feat(ai): recommendations schemas + read-only repository"
```

---

## Task 2.2: Service (prompt + Ollama rank + deterministic fallback)

**Files:**
- Create: `ai/ai-microservie/app/modules/recommendations/service.py`
- Test: `ai/ai-microservie/tests/test_recommendations_service.py`

- [ ] **Step 1: Write the failing test**

Create `ai/ai-microservie/tests/test_recommendations_service.py`:

```python
import pytest

from app.core.ollama_client import OllamaUnavailable
from app.modules.recommendations.schemas import ProductRow, ProfileRow
from app.modules.recommendations.service import build_recommendations


class FakeOllama:
    def __init__(self, result=None, fail=False):
        self._result = result
        self._fail = fail

    async def generate_json(self, prompt: str) -> dict:
        if self._fail:
            raise OllamaUnavailable("down")
        return self._result


PROFILE = ProfileRow(
    user_id="u1", living_area="DORMITORY", eating_habit="COOKING",
    home_package_frequency="MONTHLY", monthly_budget=1500.0, dorm_id=None,
)
PRODUCTS = [
    ProductRow("p1", "Rice", 9.5, "FOOD", "Acme", False),
    ProductRow("p2", "Desk", 200.0, "FURNITURE", "PartnerCo", True),
    ProductRow("p3", "Phone", 999.0, "ELECTRONICS", "Acme", False),
]


@pytest.mark.asyncio
async def test_llm_path_uses_model_ranking():
    ollama = FakeOllama(result={
        "recommendations": [
            {"productId": "p2", "reason": "Dorm desk fits your setup"},
            {"productId": "p1", "reason": "Cheap staple for cooking"},
        ]
    })
    items, source = await build_recommendations(PROFILE, PRODUCTS, ollama)
    assert source == "llm"
    assert [i.productId for i in items] == ["p2", "p1"]
    assert items[0].name == "Desk" and items[0].reason


@pytest.mark.asyncio
async def test_llm_unknown_ids_are_dropped():
    ollama = FakeOllama(result={"recommendations": [
        {"productId": "ghost", "reason": "x"},
        {"productId": "p1", "reason": "ok"},
    ]})
    items, source = await build_recommendations(PROFILE, PRODUCTS, ollama)
    assert [i.productId for i in items] == ["p1"]


@pytest.mark.asyncio
async def test_fallback_when_ollama_unavailable():
    ollama = FakeOllama(fail=True)
    items, source = await build_recommendations(PROFILE, PRODUCTS, ollama)
    assert source == "fallback"
    # DORMITORY+COOKING needs HOUSEHOLD/FOOD/FURNITURE → FOOD & FURNITURE
    # in-category first; partner before non-partner; cheaper first.
    assert items[0].productId == "p2"  # FURNITURE, partner
    assert "p1" in [i.productId for i in items]  # FOOD
    assert all(i.reason for i in items)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ai/ai-microservie && pytest tests/test_recommendations_service.py -v`
Expected: FAIL — `service` does not exist.

- [ ] **Step 3: Create `app/modules/recommendations/service.py`**

```python
import json

from app.core.ollama_client import OllamaUnavailable
from app.modules.recommendations.schemas import (
    ProductRow,
    ProfileRow,
    RecommendationItem,
)

MAX_RESULTS = 10

# Deterministic profile -> needed catalog categories (mirrors
# .agents/rules/data-model.md). Tunable in one place.
_LIVING_AREA_CATS = {
    "DORMITORY": {"HOUSEHOLD", "FOOD", "FURNITURE"},
    "RENT": {"FURNITURE", "HOUSEHOLD", "FOOD"},
    "OWN_HOME": {"FOOD", "ELECTRONICS"},
    "COMMUTER": {"FOOD", "ELECTRONICS"},
}
_EATING_CATS = {
    "COOKING": {"FOOD", "HOUSEHOLD"},
    "CANTEEN": {"FOOD", "OTHER"},
    "DELIVERY": {"FOOD", "OTHER"},
    "EATING_OUT": {"FOOD", "OTHER"},
}
_BASELINE_CATS = {"ELECTRONICS", "OTHER"}


def needed_categories(profile: ProfileRow) -> set[str]:
    cats: set[str] = set(_BASELINE_CATS)
    if profile.living_area:
        cats |= _LIVING_AREA_CATS.get(profile.living_area, set())
    if profile.eating_habit:
        cats |= _EATING_CATS.get(profile.eating_habit, set())
    return cats


def _prompt(profile: ProfileRow, products: list[ProductRow]) -> str:
    catalog = [
        {
            "productId": p.product_id,
            "name": p.name,
            "price": p.price,
            "category": p.category,
            "store": p.store_name,
            "isPartner": p.is_partner,
        }
        for p in products
    ]
    return (
        "You are a student budgeting assistant. Pick the most useful "
        "products for this student and rank them best-first. Reply ONLY "
        "with JSON: {\"recommendations\":[{\"productId\":\"..\","
        "\"reason\":\"one short sentence\"}]}.\n\n"
        f"STUDENT_PROFILE: {json.dumps(_profile_dict(profile))}\n"
        f"CATALOG: {json.dumps(catalog)}\n"
    )


def _profile_dict(profile: ProfileRow) -> dict:
    return {
        "livingArea": profile.living_area,
        "eatingHabit": profile.eating_habit,
        "homePackageFrequency": profile.home_package_frequency,
        "monthlyBudget": profile.monthly_budget,
        "fixedExpenses": [
            {"name": n, "amount": a} for n, a in profile.fixed_expenses
        ],
    }


def _item(p: ProductRow, reason: str) -> RecommendationItem:
    return RecommendationItem(
        productId=p.product_id,
        name=p.name,
        price=p.price,
        category=p.category,
        storeName=p.store_name,
        isPartner=p.is_partner,
        reason=reason,
    )


def _fallback(
    profile: ProfileRow, products: list[ProductRow]
) -> list[RecommendationItem]:
    needed = needed_categories(profile)
    ranked = sorted(
        products,
        key=lambda p: (
            0 if p.category in needed else 1,
            0 if p.is_partner else 1,
            p.price,
        ),
    )
    reason = "Matches your profile (category fit, partner store, lower price)."
    return [_item(p, reason) for p in ranked[:MAX_RESULTS]]


async def build_recommendations(
    profile: ProfileRow, products: list[ProductRow], ollama
) -> tuple[list[RecommendationItem], str]:
    by_id = {p.product_id: p for p in products}
    try:
        data = await ollama.generate_json(_prompt(profile, products))
    except OllamaUnavailable:
        return _fallback(profile, products), "fallback"

    picked: list[RecommendationItem] = []
    for entry in data.get("recommendations", []):
        pid = str(entry.get("productId", ""))
        product = by_id.get(pid)
        if product is None:
            continue
        reason = str(entry.get("reason") or "Recommended for your profile.")
        picked.append(_item(product, reason))
        if len(picked) >= MAX_RESULTS:
            break

    if not picked:
        return _fallback(profile, products), "fallback"
    return picked, "llm"
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd ai/ai-microservie && pytest tests/test_recommendations_service.py -v`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git -C ai add ai-microservie/app/modules/recommendations/service.py \
  ai-microservie/tests/test_recommendations_service.py
git -C ai commit -m "feat(ai): recommendations service (Ollama rank + deterministic fallback)"
```

---

## Task 2.3: Recommendations router + wiring

**Files:**
- Create: `ai/ai-microservie/app/api/v1/recommendations.py`
- Modify: `ai/ai-microservie/app/main.py`
- Test: `ai/ai-microservie/tests/test_recommendations_api.py`

- [ ] **Step 1: Write the failing test**

Create `ai/ai-microservie/tests/test_recommendations_api.py`:

```python
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ai/ai-microservie && pytest tests/test_recommendations_api.py -v`
Expected: FAIL — `app.api.v1.recommendations` does not exist.

- [ ] **Step 3: Create `app/api/v1/recommendations.py`**

```python
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.engine import Connection

from app.core.db import get_db
from app.core.ollama_client import get_ollama_client
from app.core.security import require_internal_secret
from app.modules.recommendations.repository import (
    load_profile,
    load_published_products,
)
from app.modules.recommendations.schemas import (
    RecommendationRequest,
    RecommendationResponse,
)
from app.modules.recommendations.service import build_recommendations

router = APIRouter(
    prefix="/api/v1", dependencies=[Depends(require_internal_secret)]
)


@router.post("/recommendations", response_model=RecommendationResponse)
async def recommendations(
    body: RecommendationRequest,
    conn: Connection = Depends(get_db),
    ollama=Depends(get_ollama_client),
) -> RecommendationResponse:
    user_id = str(body.userId)
    profile = load_profile(conn, user_id)
    if profile is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No profile for user",
        )
    products = load_published_products(conn)
    items, source = await build_recommendations(profile, products, ollama)
    return RecommendationResponse(
        userId=user_id, source=source, recommendations=items
    )
```

- [ ] **Step 4: Mount the router in `app/main.py`**

Replace `app/main.py` entirely:

```python
from fastapi import APIRouter, Depends, FastAPI

from app.api.v1.health import router as health_router
from app.api.v1.recommendations import router as recommendations_router
from app.core.security import require_internal_secret

_guard_probe = APIRouter(
    prefix="/api/v1", dependencies=[Depends(require_internal_secret)]
)


@_guard_probe.get("/_guard_probe")
def _guard_probe_route() -> dict:
    return {"ok": True}


def create_app() -> FastAPI:
    app = FastAPI(title="Student Compass AI", version="0.1.0")
    app.include_router(health_router)
    app.include_router(_guard_probe)
    app.include_router(recommendations_router)
    return app


app = create_app()
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd ai/ai-microservie && pytest tests/test_recommendations_api.py -v`
Expected: PASS (4 tests).

- [ ] **Step 6: Run the full suite (no regression)**

Run: `cd ai/ai-microservie && pytest -v`
Expected: PASS (all tests so far).

- [ ] **Step 7: Commit**

```bash
git -C ai add ai-microservie/app/api/v1/recommendations.py \
  ai-microservie/app/main.py \
  ai-microservie/tests/test_recommendations_api.py
git -C ai commit -m "feat(ai): POST /api/v1/recommendations (guarded, db+ollama)"
```

---

# Phase 3 — Bank module

## Task 3.1: PDF text extraction (in-memory)

**Files:**
- Create: `ai/ai-microservie/app/modules/bank/__init__.py` (empty)
- Create: `ai/ai-microservie/app/modules/bank/pdf.py`
- Test: `ai/ai-microservie/tests/test_bank_pdf.py`

- [ ] **Step 1: Write the failing test**

Create `ai/ai-microservie/tests/test_bank_pdf.py`:

```python
import fitz
import pytest

from app.modules.bank.pdf import EmptyPdfText, extract_text


def _pdf_with_text(text: str) -> bytes:
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


def test_extract_text_returns_layer():
    out = extract_text(_pdf_with_text("2026-03-01 LIDL -42.10"))
    assert "LIDL" in out


def test_empty_text_layer_raises():
    with pytest.raises(EmptyPdfText):
        extract_text(_pdf_no_text())
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ai/ai-microservie && pytest tests/test_bank_pdf.py -v`
Expected: FAIL — `app.modules.bank.pdf` does not exist.

- [ ] **Step 3: Create `app/modules/bank/__init__.py`** (empty file)

- [ ] **Step 4: Create `app/modules/bank/pdf.py`**

```python
import fitz  # PyMuPDF


class EmptyPdfText(ValueError):
    """The PDF has no extractable text layer (scanned image / empty)."""


def extract_text(data: bytes) -> str:
    """Extract the text layer from PDF bytes, fully in-memory.

    Never writes to disk. The caller must not log the return value.
    """
    text_parts: list[str] = []
    doc = fitz.open(stream=data, filetype="pdf")
    try:
        for page in doc:
            text_parts.append(page.get_text("text"))
    finally:
        doc.close()
    text = "\n".join(text_parts).strip()
    if not text:
        raise EmptyPdfText("PDF has no extractable text layer")
    return text
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd ai/ai-microservie && pytest tests/test_bank_pdf.py -v`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git -C ai add ai-microservie/app/modules/bank/__init__.py \
  ai-microservie/app/modules/bank/pdf.py \
  ai-microservie/tests/test_bank_pdf.py
git -C ai commit -m "feat(ai): in-memory PDF text extraction (empty layer -> error)"
```

---

## Task 3.2: Bank schemas + service

**Files:**
- Create: `ai/ai-microservie/app/modules/bank/schemas.py`
- Create: `ai/ai-microservie/app/modules/bank/service.py`
- Test: `ai/ai-microservie/tests/test_bank_service.py`

- [ ] **Step 1: Write the failing test**

Create `ai/ai-microservie/tests/test_bank_service.py`:

```python
import pytest

from app.core.ollama_client import OllamaUnavailable
from app.modules.bank.service import analyze_text


class FakeOllama:
    def __init__(self, result=None, fail=False):
        self._result = result
        self._fail = fail

    async def generate_json(self, prompt):
        if self._fail:
            raise OllamaUnavailable("down")
        return self._result


@pytest.mark.asyncio
async def test_analyze_maps_model_output():
    ollama = FakeOllama(result={
        "periodLabel": "Mar 2026",
        "totalSpent": 162.10,
        "categories": [{"category": "FOOD", "amount": 42.10}],
        "questions": [
            {"prompt": "One-off?", "merchant": "EMAG",
             "amount": 120.0, "isImpulsive": True}
        ],
    })
    res = await analyze_text("2026-03-01 LIDL -42.10", ollama)
    assert res.insights.totalSpent == 162.10
    assert res.insights.periodLabel == "Mar 2026"
    assert res.categories[0].category == "FOOD"
    assert res.questions[0].isImpulsive is True


@pytest.mark.asyncio
async def test_ollama_unavailable_propagates():
    with pytest.raises(OllamaUnavailable):
        await analyze_text("x", FakeOllama(fail=True))
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ai/ai-microservie && pytest tests/test_bank_service.py -v`
Expected: FAIL — `app.modules.bank.service` does not exist.

- [ ] **Step 3: Create `app/modules/bank/schemas.py`**

```python
from pydantic import BaseModel


class BankInsights(BaseModel):
    totalSpent: float
    periodLabel: str


class CategoryAmount(BaseModel):
    category: str
    amount: float


class ClarifyingQuestion(BaseModel):
    prompt: str
    merchant: str
    amount: float
    isImpulsive: bool


class BankAnalyzeResponse(BaseModel):
    insights: BankInsights
    categories: list[CategoryAmount]
    questions: list[ClarifyingQuestion]
```

- [ ] **Step 4: Create `app/modules/bank/service.py`**

```python
import json

from app.modules.bank.schemas import (
    BankAnalyzeResponse,
    BankInsights,
    CategoryAmount,
    ClarifyingQuestion,
)

_PROMPT = (
    "You analyze a bank statement's raw text. Return ONLY JSON: "
    '{"periodLabel":"e.g. Mar 2026","totalSpent":<number>,'
    '"categories":[{"category":"FOOD|TRANSPORT|HOUSING|SUPPLIES|'
    'PERSONAL|LEISURE|OTHER","amount":<number>}],'
    '"questions":[{"prompt":"short question","merchant":"name",'
    '"amount":<number>,"isImpulsive":true|false}]}. '
    "Flag large, odd-hour, or repeated discretionary spend as impulsive. "
    "Do NOT echo raw transaction lines.\n\nSTATEMENT_TEXT:\n"
)


async def analyze_text(text: str, ollama) -> BankAnalyzeResponse:
    """Structure statement text into derived insights only.

    Raises OllamaUnavailable if the model is unreachable (caller -> 503).
    Never returns or logs raw transaction lines.
    """
    data = await ollama.generate_json(_PROMPT + text)
    return BankAnalyzeResponse(
        insights=BankInsights(
            totalSpent=float(data.get("totalSpent", 0.0)),
            periodLabel=str(data.get("periodLabel", "Unknown")),
        ),
        categories=[
            CategoryAmount(
                category=str(c.get("category", "OTHER")),
                amount=float(c.get("amount", 0.0)),
            )
            for c in data.get("categories", [])
        ],
        questions=[
            ClarifyingQuestion(
                prompt=str(q.get("prompt", "")),
                merchant=str(q.get("merchant", "")),
                amount=float(q.get("amount", 0.0)),
                isImpulsive=bool(q.get("isImpulsive", False)),
            )
            for q in data.get("questions", [])
        ],
    )
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd ai/ai-microservie && pytest tests/test_bank_service.py -v`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git -C ai add ai-microservie/app/modules/bank/schemas.py \
  ai-microservie/app/modules/bank/service.py \
  ai-microservie/tests/test_bank_service.py
git -C ai commit -m "feat(ai): bank service maps Ollama output to derived insights"
```

---

## Task 3.3: Bank router + wiring + privacy test

**Files:**
- Create: `ai/ai-microservie/app/api/v1/bank.py`
- Modify: `ai/ai-microservie/app/main.py`
- Test: `ai/ai-microservie/tests/test_bank_api.py`

- [ ] **Step 1: Write the failing test**

Create `ai/ai-microservie/tests/test_bank_api.py`:

```python
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd ai/ai-microservie && pytest tests/test_bank_api.py -v`
Expected: FAIL — `app.api.v1.bank` does not exist.

- [ ] **Step 3: Create `app/api/v1/bank.py`**

```python
from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    UploadFile,
    status,
)

from app.core.ollama_client import OllamaUnavailable, get_ollama_client
from app.core.security import require_internal_secret
from app.modules.bank.pdf import EmptyPdfText, extract_text
from app.modules.bank.schemas import BankAnalyzeResponse
from app.modules.bank.service import analyze_text

router = APIRouter(
    prefix="/api/v1", dependencies=[Depends(require_internal_secret)]
)


@router.post("/bank/analyze", response_model=BankAnalyzeResponse)
async def bank_analyze(
    file: UploadFile,
    ollama=Depends(get_ollama_client),
) -> BankAnalyzeResponse:
    data = await file.read()  # bytes stay in memory only
    try:
        text = extract_text(data)
    except EmptyPdfText:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="PDF has no extractable text layer",
        )
    try:
        return await analyze_text(text, ollama)
    except OllamaUnavailable:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Analysis model unavailable",
        )
```

- [ ] **Step 4: Mount the router in `app/main.py`**

Replace `app/main.py` entirely:

```python
from fastapi import APIRouter, Depends, FastAPI

from app.api.v1.bank import router as bank_router
from app.api.v1.health import router as health_router
from app.api.v1.recommendations import router as recommendations_router
from app.core.security import require_internal_secret

_guard_probe = APIRouter(
    prefix="/api/v1", dependencies=[Depends(require_internal_secret)]
)


@_guard_probe.get("/_guard_probe")
def _guard_probe_route() -> dict:
    return {"ok": True}


def create_app() -> FastAPI:
    app = FastAPI(title="Student Compass AI", version="0.1.0")
    app.include_router(health_router)
    app.include_router(_guard_probe)
    app.include_router(recommendations_router)
    app.include_router(bank_router)
    return app


app = create_app()
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd ai/ai-microservie && pytest tests/test_bank_api.py -v`
Expected: PASS (4 tests).

- [ ] **Step 6: Run the full suite (no regression)**

Run: `cd ai/ai-microservie && pytest -v`
Expected: PASS (all tests).

- [ ] **Step 7: Commit**

```bash
git -C ai add ai-microservie/app/api/v1/bank.py ai-microservie/app/main.py \
  ai-microservie/tests/test_bank_api.py
git -C ai commit -m "feat(ai): POST /api/v1/bank/analyze (guarded, insights-only, 422/503)"
```

---

# Phase 4 — Containerization (Ollama + llama3 baked in)

## Task 4.1: Entrypoint + Dockerfile + compose

**Files:**
- Create: `ai/ai-microservie/entrypoint.sh`
- Modify: `ai/ai-microservie/Dockerfile`
- Modify: `ai/ai-microservie/docker-compose.yml`

- [ ] **Step 1: Create `entrypoint.sh`**

```bash
#!/usr/bin/env bash
set -euo pipefail

ollama serve &
OLLAMA_PID=$!

# Wait for the in-container Ollama to accept connections.
for i in $(seq 1 60); do
  if curl -sf http://localhost:11434/api/tags >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

trap 'kill "$OLLAMA_PID" 2>/dev/null || true' EXIT
exec uvicorn app.main:app --host 0.0.0.0 --port "${PORT:-3000}"
```

- [ ] **Step 2: Replace `Dockerfile`**

```dockerfile
FROM python:3.11-slim

# Ollama + curl (healthcheck/wait) + build deps for psycopg2/pymupdf wheels.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && curl -fsSL https://ollama.com/install.sh | sh \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Bake the llama3 model into the image (multi-GB, accepted trade-off).
RUN ollama serve & \
    for i in $(seq 1 60); do \
      curl -sf http://localhost:11434/api/tags >/dev/null 2>&1 && break; \
      sleep 1; \
    done; \
    ollama pull llama3; \
    pkill ollama || true

COPY app ./app
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

EXPOSE 3000
ENV PORT=3000

CMD ["./entrypoint.sh"]
```

- [ ] **Step 3: Replace `docker-compose.yml`**

```yaml
version: "3.9"

services:
  ai:
    build: .
    ports:
      - "3000:3000"
    environment:
      - PORT=3000
      - AI_SERVICE_SHARED_SECRET=${AI_SERVICE_SHARED_SECRET:-dev-internal-secret}
      - DATABASE_URL=${DATABASE_URL:-sqlite+pysqlite:///:memory:}
      - OLLAMA_BASE_URL=http://localhost:11434
      - OLLAMA_MODEL=llama3
```

- [ ] **Step 4: Build the image**

Run: `cd ai/ai-microservie && docker build -t stufi-ai .`
Expected: build succeeds. NOTE: large/slow — pulls `llama3` (multi-GB) during build, as designed. If the environment cannot build images, document this and defer to the machine that will run it.

- [ ] **Step 5: Smoke-run health**

Run:
```bash
docker run -d --name stufi-ai -p 3000:3000 stufi-ai
sleep 20
curl -s http://localhost:3000/health
docker rm -f stufi-ai
```
Expected: `{"status":"ok"}`.

- [ ] **Step 6: Commit**

```bash
git -C ai add ai-microservie/entrypoint.sh ai-microservie/Dockerfile \
  ai-microservie/docker-compose.yml
git -C ai commit -m "build(ai): bake Ollama+llama3 into image; entrypoint runs both"
```

---

# Phase 5 — Docs: override record + backend suggestions

## Task 5.1: Record the decisions override inside ai/

**Files:**
- Create: `ai/docs/decisions-override.md`

- [ ] **Step 1: Create `ai/docs/decisions-override.md`**

```markdown
# Decisions override (recorded inside ai/)

Per product-owner decision 2026-05-16. `.agents/memory/decisions.md` is
intentionally **not edited** (outside `ai/`); see the recommendation in
`backend-integration-suggestions.md`.

- **#1/#6:** Recommendations are LLM-powered via local Ollama `llama3`.
  There is **no chat** module. Anthropic/Claude is not used.
- **#2/#3:** The AI service has **read-only** access to the shared Postgres
  DB. It still writes nothing and the backend keeps sole ownership of the
  schema and Flyway migrations.
- **#5:** Bank structuring via local Ollama — unchanged. Raw PDF/transaction
  lines are never persisted, logged, or returned (insights only).
```

- [ ] **Step 2: Commit**

```bash
git -C ai add ai-microservie/../docs/decisions-override.md
git -C ai commit -m "docs(ai): record decisions override inside ai/"
```

---

## Task 5.2: Backend integration suggestion doc (not applied)

**Files:**
- Create: `ai/docs/backend-integration-suggestions.md`

- [ ] **Step 1: Create `ai/docs/backend-integration-suggestions.md`**

```markdown
# Backend integration suggestions (review & permit before applying)

The AI service changes are complete in `ai/`. The following backend (`back/`)
changes are **proposed only** — not applied. Apply at your discretion.

## 1. Verify the existing backend `ai` package
Confirm there is a single client module targeting `AI_SERVICE_URL` that sends
`X-Internal-Secret: ${AI_SERVICE_SHARED_SECRET}` on every call, with sane
timeouts and clear error mapping (502/504 -> user-facing message).

## 2. Recommendations flow (front -> back -> ai -> back -> front)
- New backend endpoint, e.g. `POST /api/v1/ai/recommendations` (JWT-protected,
  owner/ADMIN). Backend resolves the caller's userId, calls
  `POST {AI}/api/v1/recommendations` with `{ "userId": "<uuid>" }` + secret.
- AI returns `{ userId, source, recommendations:[{productId,name,price,
  category,storeName,isPartner,reason}] }`. Backend relays to the front.

## 3. Bank flow (front -> back metadata-only -> ai -> back -> front signal)
- `POST /api/v1/bank/analyze` (multipart). Backend stores **metadata only**
  (no raw bytes/lines), streams the PDF bytes to
  `POST {AI}/api/v1/bank/analyze` + secret.
- AI returns derived insights only:
  `{ insights:{totalSpent,periodLabel}, categories:[...], questions:[...] }`.
  Backend persists/relays per its own privacy rules and signals the front.
- AI responses: `422` (no text layer), `503` (model unavailable) — map to
  clear user messaging.

## 4. Read-only DB role (required for recommendations)
Provision a Postgres role with `SELECT` on `student_profiles`,
`student_fixed_expenses`, `stores`, `catalog_products` and pass its DSN to the
AI service as `DATABASE_URL`. The AI never writes or migrates.

## 5. HARD DEPENDENCY — catalog migration
Recommendations reads `catalog_products` (status='PUBLISHED') joined to
`stores(is_partner)`. These are **Group B** tables that do not exist yet.
Backend must add the catalog Flyway migration (`stores`, `catalog_products`
per `.agents/rules/data-model.md`) before recommendations works against the
real DB. Until then the endpoint returns an empty list (no published rows).

## 6. Env wiring
- `back`: `AI_SERVICE_URL`, `AI_SERVICE_SHARED_SECRET`.
- `ai`: `AI_SERVICE_SHARED_SECRET`, `DATABASE_URL` (read-only),
  `OLLAMA_BASE_URL`, `OLLAMA_MODEL`. Never commit secrets.

## 7. Recommended `.agents/memory/decisions.md` edit (outside ai/, not applied)
Record the overrides from `ai/docs/decisions-override.md` in
`.agents/memory/decisions.md` so the rest of the project stays consistent:
recommendations are now Ollama-powered (no chat); the AI service has
read-only shared-DB access (writes nothing; backend owns schema/Flyway).
```

- [ ] **Step 2: Final full-suite run**

Run: `cd ai/ai-microservie && pytest -v`
Expected: PASS (all tests, all phases).

- [ ] **Step 3: Commit**

```bash
git -C ai add ai-microservie/../docs/backend-integration-suggestions.md
git -C ai commit -m "docs(ai): backend integration suggestions (not applied)"
```

---

## Self-review

**1. Spec coverage** (against `2026-05-16-ai-microservice-design.md`):
- §2 scope (recommendations + bank + infra; no scraper/chat/anthropic) → Phases 0–3.
- §3 overrides recorded inside ai/ → Task 5.1; decisions.md edit recommended only → Task 5.2 §7.
- §5.1 config → Task 0.2. §5.2 guard (401, /health public) → Task 0.3. §5.3 read-only Core → Task 1.1. §5.4 Ollama client → Task 1.2.
- §6 recommendations (`{userId}`, read profile+catalog, llm rank, fallback, 404) → Tasks 2.1–2.3.
- §6 catalog dependency documented → Task 5.2 §5.
- §7 bank (multipart, 422 empty, insights-only, 503, no-leak test) → Tasks 3.1–3.3.
- §8 Docker (Ollama+llama3 baked, entrypoint) → Task 4.1.
- §9 deps (httpx/pymupdf/sqlalchemy/psycopg2/pytest; no anthropic) → Task 0.1.
- §10 testing (security/recommendations/bank/health) → tests in every task.
- §11 backend suggestion doc → Task 5.2.

**2. Placeholder scan:** No TBD/TODO. Every code step shows full code; every command states expected output. The `_guard_probe` route is intentionally kept (cheap, exercises the guard in tests).

**3. Type consistency:** `ProfileRow`/`ProductRow` defined in `recommendations/schemas.py` (Task 2.1), consumed unchanged by `repository.py` (2.1) and `service.py` (2.2). `build_recommendations(profile, products, ollama) -> (list[RecommendationItem], str)` signature consistent across 2.2/2.3. `OllamaClient.generate_json` signature consistent across 1.2/2.2/3.2 and all fakes. `extract_text`/`EmptyPdfText` consistent across 3.1/3.3. `get_db`/`get_ollama_client` dependency names consistent across db.py/ollama_client.py and every router + test override. `main.py` is fully replaced each time it changes (0.3 → 2.3 → 3.3) so no partial-edit drift.

**4. Scope:** One cohesive service; phases are independently testable and ship working software (Phase 0–3 demoable via TestClient even before Docker/backend).
