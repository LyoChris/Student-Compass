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
