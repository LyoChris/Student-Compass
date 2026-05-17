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
  category,storeName,isPartner,reason}] }`. `source` is `"llm"` or
  `"fallback"`. Backend relays to the front.

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
