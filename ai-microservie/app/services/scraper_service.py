# services/scraper_service.py

from typing import List, Dict

from modules.scraper import auchan_scraper, dedeman_scraper, emag_scraper
from app.core.scraper.storage import save_products_to_csv

# Map scraperType to function
SCRAPER_DISPATCHER = {
    "AUCHAN_JSONLD": auchan_scraper.scrape_auchan_category,
    "DEDEMAN_HTML": dedeman_scraper.scrape_dedeman_category,
    "EMAG_HTML": emag_scraper.scrape_emag_category,
}


def run_scraping_job(request) -> Dict:
    """
    Orchestrates scraping for multiple sources and returns CSV as Base64.
    `request` is expected to be a Pydantic model ScrapeRequest
    """
    all_products = []
    seen_global = set()
    errors = []

    for source in request.sources:
        scraper_type = source.scraperType
        func = SCRAPER_DISPATCHER.get(scraper_type)
        if not func:
            errors.append(f"Unsupported scraperType: {scraper_type} for source {source.categoryName}")
            continue

        try:
            products: List[Dict] = func(
                category_url=str(source.url),
                category_name=source.categoryName,
                max_pages=request.maxPagesPerCategory
            )
        except Exception as e:
            errors.append(f"Error scraping {source.categoryName}: {str(e)}")
            continue

        for p in products:
            key = p.get("product_url") or p.get("sku") or p.get("name")
            if key and key not in seen_global:
                seen_global.add(key)
                all_products.append(p)

    if not all_products and errors:
        return {
            "status": "failed",
            "productsCount": 0,
            "errors": errors
        }

    if all_products:
        save_products_to_csv(all_products)

    return {
        "status": "success" if not errors else "partial_success",
        "productsCount": len(all_products),
        "errors": errors
    }
