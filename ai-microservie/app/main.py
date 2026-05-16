from fastapi import APIRouter, Depends, FastAPI

from app.api.v1.health import router as health_router
from app.core.security import require_internal_secret

_guard_probe = APIRouter(
    prefix="/api/v1", dependencies=[Depends(require_internal_secret)]
)


@_guard_probe.get("/_guard_probe")
def _guard_probe_route() -> dict:
    return {"ok": True}


def create_app() -> FastAPI:
    app = FastAPI(title="Student Compass AI", version="0.1.0")
    app.include_router(health_router)
    app.include_router(_guard_probe)
    return app


app = create_app()
