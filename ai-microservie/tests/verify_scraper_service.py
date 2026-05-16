
import sys
import os
from unittest.mock import MagicMock

# Setup path
sys.path.append(os.path.join(os.getcwd(), "ai-microservie"))
sys.path.append(os.path.join(os.getcwd(), "ai-microservie", "app"))

from services.scraper_service import run_scraping_job

def test_run_scraping_job_no_products():
    request = MagicMock()
    request.sources = []
    request.maxPagesPerCategory = 2
    
    result = run_scraping_job(request)
    
    assert result["status"] == "success"
    assert result["productsCount"] == 0
    assert "csvBase64" not in result
    assert "csvFileName" not in result
    print("test_run_scraping_job_no_products passed")

def test_run_scraping_job_with_products():
    # Mock storage
    import services.scraper_service
    services.scraper_service.save_products_to_csv = MagicMock()
    
    # Mock dispatcher
    services.scraper_service.SCRAPER_DISPATCHER = {
        "TEST": lambda category_url, category_name, max_pages: [{"name": "Test product", "category_name": "Test"}]
    }
    
    request = MagicMock()
    source = MagicMock()
    source.scraperType = "TEST"
    source.categoryName = "Test"
    source.url = "http://test.com"
    request.sources = [source]
    request.maxPagesPerCategory = 1
    
    result = run_scraping_job(request)
    
    assert result["status"] == "success"
    assert result["productsCount"] == 1
    assert "csvBase64" not in result
    assert "csvFileName" not in result
    services.scraper_service.save_products_to_csv.assert_called_once()
    print("test_run_scraping_job_with_products passed")

if __name__ == "__main__":
    try:
        test_run_scraping_job_no_products()
        test_run_scraping_job_with_products()
        print("All tests passed!")
    except Exception as e:
        print(f"Test failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
