import secrets

from fastapi import Header, HTTPException, status

from app.core.config import settings


async def require_internal_secret(
    x_internal_secret: str | None = Header(default=None),
) -> None:
    """Reject any request that does not present the shared internal secret.

    Applied to every /api/v1 router. /health stays public.
    """
    expected = settings.ai_service_shared_secret
    if not x_internal_secret or not secrets.compare_digest(
        x_internal_secret, expected
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing internal secret",
        )
