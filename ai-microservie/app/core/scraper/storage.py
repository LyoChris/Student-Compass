import os
import csv
from typing import List, Dict
from app.core.scraper.config import settings

def save_products_to_csv(products: List[Dict]):
    """
    Groups products by category folder and saves them to products.csv.
    """
    # Group products by target folder
    grouped_products = {}
    for p in products:
        category = p.get("category_name", "Unknown")
        folder = settings.CATEGORY_TO_FOLDER_MAP.get(category, "uncategorized")
        if folder not in grouped_products:
            grouped_products[folder] = []
        grouped_products[folder].append(p)
    
    fieldnames = [
        "category_name", "position", "id", "name", "description", "brand",
        "sku", "mpn", "price_ron", "low_price_ron", "high_price_ron",
        "currency", "availability", "image_url", "product_url",
        "shop_name", "city", "category_url"
    ]
    
    for folder, folder_products in grouped_products.items():
        dir_path = os.path.join(settings.DATA_STORAGE_PATH, folder)
        os.makedirs(dir_path, exist_ok=True)
        
        file_path = os.path.join(dir_path, "products.csv")
        
        with open(file_path, mode='w', encoding='utf-8', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction='ignore')
            writer.writeheader()
            for p in folder_products:
                writer.writerow(p)
