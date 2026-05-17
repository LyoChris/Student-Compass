# models/scraper/scraper_request.py

from pydantic import BaseModel, HttpUrl
from typing import List, Literal


class ScraperSource(BaseModel):
    id: int
    shopName: str
    categoryName: str
    url: HttpUrl
    scraperType: Literal["AUCHAN_JSONLD", "DEDEMAN_HTML", "EMAG_HTML"]


class ScrapeRequest(BaseModel):
    sources: List[ScraperSource]
    maxPagesPerCategory: int = 2
    outputFormat: Literal["csv", "json"] = "csv"
