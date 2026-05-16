# main.py

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from api.scraper.scraper import router as scraping_router

app = FastAPI(
    title="Python Scraper Service",
    description="Service for scraping products from predefined shops and returning CSV as Base64",
    version="1.0.0"
)

# CORS middleware - allow Spring Boot frontend to call the service
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

# Include the scraping router
app.include_router(scraping_router)

# Health check endpoint
@app.get("/health", summary="Health check", description="Returns service status")
def health_check():
    return {"status": "ok"}
