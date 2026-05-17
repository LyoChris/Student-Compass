from dataclasses import dataclass, field
from typing import Literal

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
    source: Literal["llm", "fallback"]
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
