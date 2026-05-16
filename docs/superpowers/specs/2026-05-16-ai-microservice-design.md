# AI Microservice — Design Spec

**Date:** 2026-05-16
**Repo:** `ai/` (own git; microservice at `ai/ai-microservie/`)
**Status:** Approved design — ready for implementation plan

---

## 1. Purpose

The `ai/` service is a FastAPI compute service for StuFi (Student Financial
Compass). It currently has only a public `GET /health`. This effort adds two
real capabilities, both powered by a **local Ollama `llama3`** model baked into
the container image:

1. **Recommendations** — suggest catalog products to a student based on their
   stored profile preferences.
2. **Bank analysis** — turn an uploaded bank-statement PDF into derived
   spending insights, privacy-first (no raw lines kept).

The scraper module is **out of scope** (owned by a colleague, not yet pushed).
Conversational chat is **out of scope** (explicitly dropped). Anthropic/Claude
is **not used** anywhere.

## 2. Scope

**In scope (modify only `ai/`):**

- `recommendations` module + `POST /api/v1/recommendations`.
- `bank` module + `POST /api/v1/bank/analyze`.
- Shared infra: shared-secret guard, pydantic-settings config, read-only DB
  layer, Ollama client.
- Dockerfile/compose with Ollama + `llama3` baked in.
- pytest test suite (TDD).
- A backend suggestion document (written, **not applied** — backend changes
  require the owner's explicit permission).

**Out of scope:** the scraper module; conversational chat; any Anthropic
integration; applying backend changes; Flyway migrations (backend owns schema).

## 3. Locked-decision overrides (recorded deliberately)

These deviate from `.agents/memory/decisions.md` by explicit product-owner
decision on 2026-05-16. The override is recorded **inside `ai/` only** (this
spec is the system of record for it). Editing `.agents/memory/decisions.md`
is **not done here** (it is outside `ai/`); the backend suggestion doc will
*recommend* that edit for the owner to apply.

| # | Original locked decision | New direction |
|---|--------------------------|---------------|
| 1 / 6 | Recommendations = deterministic Java rules; LLM only powers chat | Recommendations are **LLM-powered via local Ollama `llama3`**; there is **no chat** |
| 2 / 3 | AI service is stateless; Spring is the *only* place with the DB | AI service has **read-only** access to the **shared Postgres DB**; it still **writes nothing** and the backend keeps **sole ownership of schema/Flyway** |
| 5 | Bank structuring via local Ollama (kept) | Unchanged — bank uses local Ollama; consistent |

Bank privacy rule (raw PDF/transaction lines never persisted, logged, or
returned) is **unchanged and non-negotiable**.

## 4. Architecture

```
Browser ─► Spring (back) ─►  ai (FastAPI, private)
                │                  │
                └─► Postgres ◄─────┘  (ai: READ-ONLY; back: owns schema + writes)
```

- The browser never calls `ai` directly. Only the backend calls it, sending
  `X-Internal-Secret: ${AI_SERVICE_SHARED_SECRET}`.
- `ai` connects to the shared Postgres with a **read-only** role and only
  `SELECT`s. It runs no migrations and writes nothing.
- Ollama (`llama3`) runs **inside the same container**, reached at
  `OLLAMA_BASE_URL` (default `http://localhost:11434`).

### Module / package layout (`ai/ai-microservie/app/`)

```
app/
  main.py                 # create_app(): mounts health + recommendations + bank routers
  core/
    config.py             # pydantic-settings (env-driven)
    security.py            # X-Internal-Secret dependency guard
    db.py                  # SQLAlchemy Core read-only engine/session
    ollama_client.py       # httpx wrapper around Ollama (JSON mode, timeout)
  api/v1/
    health.py             # existing, public, unchanged
    recommendations.py     # POST /api/v1/recommendations
    bank.py                # POST /api/v1/bank/analyze
  modules/
    recommendations/
      schemas.py           # pydantic request/response models
      repository.py        # read-only SELECTs (profile, catalog_products, stores)
      service.py           # build prompt, call Ollama, rank, deterministic fallback
    bank/
      schemas.py           # pydantic response models
      pdf.py               # PyMuPDF in-memory text extraction
      service.py           # structure via Ollama, categorize, flag, questions
```

The placeholder folders `app/modules/{housing,bureaucracy,community,accounts,
budget}` are noise and will be left untouched (or removed if trivially safe;
default: leave them).

## 5. Component: shared infrastructure

### 5.1 `core/config.py` (pydantic-settings)

| Setting | Env | Default | Purpose |
|---------|-----|---------|---------|
| `ai_service_shared_secret` | `AI_SERVICE_SHARED_SECRET` | _required_ | guard value |
| `database_url` | `DATABASE_URL` | _required_ | read-only Postgres DSN |
| `ollama_base_url` | `OLLAMA_BASE_URL` | `http://localhost:11434` | in-container Ollama |
| `ollama_model` | `OLLAMA_MODEL` | `llama3` | model tag |
| `ollama_timeout_seconds` | `OLLAMA_TIMEOUT_SECONDS` | `60` | per-call timeout |
| `app_name` | — | `Student Compass AI` | title |

No secrets committed; all via env.

### 5.2 `core/security.py`

A FastAPI dependency `require_internal_secret` that compares the
`X-Internal-Secret` request header (constant-time) to
`settings.ai_service_shared_secret`. Missing/mismatch → `401`. Applied to the
**recommendations** and **bank** routers. **`/health` stays public** (used by
backend/frontend connectivity checks).

### 5.3 `core/db.py`

SQLAlchemy **Core** engine created from `DATABASE_URL`, used only for `SELECT`.
Lightweight `Table`/`text()` read models for the few columns needed — no ORM
entity ownership, no `create_all`, no writes. A session/connection helper
yields a connection per request and closes it.

### 5.4 `core/ollama_client.py`

Thin async `httpx` client around the Ollama HTTP API (`/api/generate` with
`format: json` / structured prompting). Enforces `ollama_timeout_seconds`.
Raises a typed `OllamaUnavailable` on connection error/timeout so callers can
choose fallback (recommendations) vs. `503` (bank).

## 6. Component: `recommendations`

**Endpoint:** `POST /api/v1/recommendations` (guarded)
**Request:** `{ "userId": "<uuid>" }`
**Response:**

```json
{
  "userId": "<uuid>",
  "source": "llm" | "fallback",
  "recommendations": [
    { "productId": "<uuid>", "name": "...", "price": 49.90,
      "category": "FOOD", "storeName": "...", "isPartner": true,
      "reason": "Short why-this-fits sentence" }
  ]
}
```

**Flow:**

1. Backend calls with `{userId}` + secret (`front → back → ai → back → front`).
2. `repository.py` reads, **read-only**:
   - `student_profiles` (+ `student_fixed_expenses`) for `userId`:
     `living_area`, `eating_habit`, `home_package_frequency`,
     `monthly_budget`, `dorm_id`, fixed-expense names/amounts.
     User not found / no profile → `404`.
   - Candidate products: `catalog_products` where `status = 'PUBLISHED'`,
     joined to `stores` for `name` + `is_partner`.
3. `service.py` builds a structured prompt (profile + candidate list) and asks
   `llama3` (JSON mode) to select & rank the best-fit products with a one-line
   `reason` each.
4. **Deterministic fallback** if Ollama is unavailable/times out or returns
   unparseable JSON: rank candidates by needed-category match → `is_partner`
   → cheapest price; `source: "fallback"`. Endpoint still returns `200`.

**Dependency (documented, not a gap):** `catalog_products` and `stores` are
**Group B** tables that **do not exist yet** (catalog module not built). The
module is implemented to the documented `data-model.md` Group B schema and
fully unit/integration-tested against a test DB that creates that minimal
schema. It is **not end-to-end demoable against the real shared DB until the
backend applies the catalog Flyway migration**. This is called out in the
backend suggestion doc.

## 7. Component: `bank`

**Endpoint:** `POST /api/v1/bank/analyze` (guarded), `multipart/form-data`,
field `file` (PDF).
**Response (derived insights only):**

```json
{
  "insights":   { "totalSpent": 1234.56, "periodLabel": "Mar 2026" },
  "categories": [ { "category": "FOOD", "amount": 420.00 } ],
  "questions":  [ { "prompt": "Was this a one-off?", "merchant": "...",
                    "amount": 199.99, "isImpulsive": true } ]
}
```

**Flow:**

1. Backend streams the PDF bytes (`front → back`, backend stores only
   metadata, redirects bytes → `ai`).
2. `pdf.py` extracts the text layer **in memory** with PyMuPDF. No OCR. Empty
   / no text layer → `422` with a clear message.
3. `service.py` sends extracted text to `llama3` to structure transactions,
   categorize, flag impulsive buys (large / odd-hour / repeated discretionary
   + model judgment), and generate clarifying questions.
4. Returns **derived insights only** — never the raw lines or structured
   per-transaction list.
5. Ollama unavailable/timeout → `503` (bank cannot be faked by a fallback).

**Privacy (hard rule, enforced & tested):** the PDF bytes and extracted text
exist only in memory for the duration of the request; never written to disk,
never logged (no `logging` of file content/text), discarded after response.
A test asserts no raw transaction string appears in the response body.

## 8. Docker / runtime

- Base image with Ollama installed.
- At **build**: start `ollama serve`, `ollama pull llama3`, bake the model
  into the image (multi-GB image and slow build are accepted trade-offs for a
  self-contained, reproducible artifact).
- Entrypoint: start `ollama serve` in the background, wait for readiness, then
  `uvicorn app.main:app`.
- `docker-compose.yml`: pass `DATABASE_URL`, `AI_SERVICE_SHARED_SECRET`,
  optional `OLLAMA_MODEL`. Expose the API port (existing `3000`).

## 9. Dependencies (`requirements.txt`, pinned)

Add: `httpx`, `pymupdf`, `sqlalchemy`, `psycopg2-binary`, `pytest`,
`pytest-asyncio` (+ an httpx mock such as `respx` for tests).
Keep: `fastapi`, `uvicorn[standard]`, `pydantic`, `pydantic-settings`.
**Not added:** `anthropic` (no Claude), HTML parser (no scraper).

## 10. Testing (pytest + TDD)

Test-first per project conventions. Suite under `ai/ai-microservie/tests/`:

- **security:** missing/wrong `X-Internal-Secret` → `401` on guarded routes;
  `/health` public `200`.
- **recommendations:** test DB with minimal `student_profiles` /
  `catalog_products` / `stores` (documented schema); Ollama mocked →
  ranked shape; Ollama failure → deterministic fallback (`source:"fallback"`,
  still `200`); unknown user → `404`.
- **bank:** small generated PDF with a text layer → mocked Ollama →
  derived-insights shape; PDF with no text layer → `422`; assert no raw
  transaction text leaks into the response; Ollama down → `503`.
- **health:** unchanged, public, `200`.

CI/local: `pytest` from `ai/ai-microservie/`.

## 11. Backend suggestion document (deliverable, not applied)

Written at the end to `ai/docs/backend-integration-suggestions.md` for the
backend owner to review and permit. Will cover:

- Verify/define the existing backend `ai` package.
- AI client: base URL `AI_SERVICE_URL`, header `X-Internal-Secret`
  (`AI_SERVICE_SHARED_SECRET`), timeouts, error mapping.
- Orchestration flows: `front → back → ai → back → front` for
  recommendations (`POST /api/v1/ai/recommendations`-style proxy with
  `{userId}`) and bank (backend stores metadata only, streams PDF to AI,
  relays a signal to the front).
- Env wiring on both sides.
- Provision a **read-only Postgres role** for the AI service.
- **Hard dependency:** backend must add the catalog Flyway migration
  (Group B: `stores`, `catalog_products`) before recommendations works
  against the real DB.
- **Recommended `.agents/memory/decisions.md` edit** (not applied by this
  effort, kept outside `ai/`): record the §3 overrides so the rest of the
  project stays consistent.

## 12. Risks & mitigations

| Risk | Mitigation |
|------|------------|
| `catalog_products` not built yet | Build to documented schema; test DB fixture; flagged dependency |
| Large Docker image / slow build (baked model) | Accepted by product owner; documented |
| Ollama cold/slow on first call | Recommendations falls back deterministically; bank returns `503` with clear message |
| Schema drift (AI reads backend-owned tables) | SQLAlchemy Core read models map only needed columns; read-only role; documented in backend doc |
| Raw bank data leak | In-memory only, no logging of content, explicit no-leak test |

## 13. Definition of done

Endpoints implemented + guarded; recommendations & bank work against mocked
Ollama and a test DB; privacy test passes; pytest green from
`ai/ai-microservie/`; Docker image builds with `llama3` baked in; backend
suggestion doc written; §3 override recorded inside `ai/` (this spec) and the
`.agents/memory/decisions.md` edit *recommended* (not applied — outside
`ai/`). No secrets committed. Backend untouched (suggestions only).
