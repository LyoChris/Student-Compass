FROM python:3.11-slim

# Ollama + curl (healthcheck/wait) for the in-container model runtime.
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
