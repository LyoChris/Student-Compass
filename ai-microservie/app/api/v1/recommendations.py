from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.engine import Connection

from app.core.db import get_db
from app.core.ollama_client import get_ollama_client
from app.core.security import require_internal_secret
from app.modules.recommendations.repository import (
    load_profile,
    load_published_products,
)
from app.modules.recommendations.schemas import (
    RecommendationRequest,
    RecommendationResponse,
)
from app.modules.recommendations.service import build_recommendations

router = APIRouter(
    prefix="/api/v1", dependencies=[Depends(require_internal_secret)]
)


@router.post("/recommendations", response_model=RecommendationResponse)
async def recommendations(
    body: RecommendationRequest,
    conn: Connection = Depends(get_db),
    ollama=Depends(get_ollama_client),
) -> RecommendationResponse:
    user_id = str(body.userId)
    profile = load_profile(conn, user_id)
    if profile is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No profile for user",
        )
    products = load_published_products(conn)
    items, source = await build_recommendations(profile, products, ollama)
    return RecommendationResponse(
        userId=user_id, source=source, recommendations=items
    )
