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
