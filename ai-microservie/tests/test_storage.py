import os
import shutil
import pytest
from app.core.scraper.storage import save_products_to_csv
from app.core.config import settings

def test_save_products_to_csv():
    # Clean up test data if exists
    test_data_dir = "test_data"
    if os.path.exists(test_data_dir):
        shutil.rmtree(test_data_dir)
    
    # Override settings for test
    original_path = settings.DATA_STORAGE_PATH
    settings.DATA_STORAGE_PATH = test_data_dir
    
    try:
        products = [
            {"name": "Pizza", "category_name": "Congelate", "price_ron": 15.0},
            {"name": "Scaun", "category_name": "Scaune birou", "price_ron": 200.0}
        ]
        
        save_products_to_csv(products)
        
        # Verify groceries file
        groceries_file = os.path.join(test_data_dir, "groceries", "products.csv")
        assert os.path.exists(groceries_file)
        
        # Verify furniture file
        furniture_file = os.path.join(test_data_dir, "furniture", "products.csv")
        assert os.path.exists(furniture_file)
        
    finally:
        # Restore settings and cleanup
        settings.DATA_STORAGE_PATH = original_path
        if os.path.exists(test_data_dir):
            shutil.rmtree(test_data_dir)
