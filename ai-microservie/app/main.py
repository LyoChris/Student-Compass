from fastapi import APIRouter, Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware

# --- Imports from HEAD (Scraper Service) ---
from app.api.scraper.scraper import router as scraping_router

# --- Imports from 'ai' branch (Student Compass) ---
from app.api.v1.health import router as health_router
from app.api.v1.recommendations import router as recommendations_router
from app.core.security import require_internal_secret

# Guard probe setup
_guard_probe = APIRouter(
    prefix="/api/v1", dependencies=[Depends(require_internal_secret)]
)

@_guard_probe.get("/_guard_probe", include_in_schema=False)
def _guard_probe_route() -> dict:
    return {"ok": True}

# App Factory
def create_app() -> FastAPI:
    app = FastAPI(
        title="Student Compass AI & Scraper Service",
        description="AI microservices and scraper for predefined shops.",
        version="0.1.0"
    )

    # CORS middleware (from HEAD) - allow Spring Boot frontend to call the service
    origins = [
        "*"  # For MVP; later restrict to Spring Boot host
    ]

    app.add_middleware(
        CORSMiddleware,
        allow_origins=origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # Include routers from BOTH branches
    app.include_router(health_router)
    app.include_router(scraping_router)
    app.include_router(_guard_probe)
    app.include_router(recommendations_router)

    return app

# Instantiate the app so uvicorn can run `main:app`
app = create_app()
