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
