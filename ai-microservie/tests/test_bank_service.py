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
