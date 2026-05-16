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
