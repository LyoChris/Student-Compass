# api/scraping.py

from fastapi import APIRouter, HTTPException
from typing import List
from pydantic import BaseModel, Field, HttpUrl
from app.services.scraper_service import run_scraping_job

router = APIRouter(
    prefix="/api",
    tags=["Scraping"]
)

# Pydantic models for API documentation
class ScraperSource(BaseModel):
    id: int = Field(..., description="Unique ID of the scraping source")
    shopName: str = Field(..., description="Name of the shop")
    categoryName: str = Field(..., description="Category name")
    url: HttpUrl = Field(..., description="URL to scrape")
    scraperType: str = Field(..., description="Type of scraper", example="AUCHAN_JSONLD")

class ScrapeRequest(BaseModel):
    sources: List[ScraperSource] = Field(..., description="List of sources to scrape")
    maxPagesPerCategory: int = Field(2, description="Max number of pages to scrape per category")
    outputFormat: str = Field("csv", description="Output format", example="csv")

class ScrapeResponse(BaseModel):
    status: str = Field(..., description="Status of the scraping job (success, partial_success, failed)")
    productsCount: int = Field(..., description="Number of products scraped")
    errors: List[str] = Field([], description="List of errors for failed sources")

# POST endpoint to trigger a scraping job
@router.post("/scraping-jobs", response_model=ScrapeResponse, summary="Trigger a scraping job", description="Trigger scraping for one or more predefined category sources and save results to categorized CSV files in the data directory.")
def create_scraping_job(request: ScrapeRequest):
    """
    Trigger a new scraping job for selected sources.

    - **sources**: List of predefined sources (shop + category + URL + scraperType)
    - **maxPagesPerCategory**: Maximum pages to scrape per category (default 2)
    - **outputFormat**: CSV or JSON (default CSV)
    """
    try:
        result = run_scraping_job(request)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# GET endpoint to list available scraping sources
@router.get("/sources", summary="Get scraping sources", description="Retrieve the list of predefined scraping sources available for admin to choose from.")
def list_scraping_sources():
    """
    Returns a list of scraping sources including:

    - **id**: unique identifier
    - **shopName**: name of the shop
    - **categoryName**: name of the category
    - **url**: link to scrape
    - **scraperType**: scraper type to use
    """
    # Example static sources, ideally this comes from DB
    sources = [
        {"id": 1, "shopName": "Auchan", "categoryName": "Congelate", "url": "https://www.auchan.ro/congelate/c", "scraperType": "AUCHAN_JSONLD"},
        {"id": 2, "shopName": "Dedeman", "categoryName": "Scaune birou", "url": "https://www.dedeman.ro/ro/scaune-birou/c/496", "scraperType": "DEDEMAN_HTML"},
        {"id": 3, "shopName": "eMAG", "categoryName": "Masini spalat", "url": "https://www.emag.ro/masini-spalat-rufe/filter/tip-incarcare-f7953/c", "scraperType": "EMAG_HTML"}
    ]
    return sources
