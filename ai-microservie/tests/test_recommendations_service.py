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
