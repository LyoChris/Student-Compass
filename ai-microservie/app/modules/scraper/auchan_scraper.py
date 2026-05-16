# Student-Compass/ai-microservie/app/modules/scraper/auchan_scraper.py

from bs4 import BeautifulSoup
from core.scraper.http_client import get_html
from core.scraper.text_utils import clean_text, parse_price
import json

SHOP_NAME = "Auchan Romania"
CITY = "Iasi"

def scrape_auchan_category(category_url: str, category_name: str, max_pages: int = 2):
    all_products = []
    seen_products = set()

    def build_page_url(page_number: int):
        if page_number == 1: return category_url
        separator = "&" if "?" in category_url else "?"
        return f"{category_url}{separator}page={page_number}"

    def extract_offer(product):
        offers = product.get("offers")
        if isinstance(offers, dict): return offers
        if isinstance(offers, list) and offers: return offers[0]
        return {}

    def extract_products_from_jsonld_object(jsonld_object, page_url):
        products = []
        if isinstance(jsonld_object, list):
            for item in jsonld_object:
                products.extend(extract_products_from_jsonld_object(item, page_url))
            return products
        if not isinstance(jsonld_object, dict): return products

        if jsonld_object.get("@type") == "ItemList":
            for list_element in jsonld_object.get("itemListElement", []):
                product = list_element.get("item")
                if not isinstance(product, dict): continue
                offer = extract_offer(product)
                products.append({
                    "category_name": category_name,
                    "position": list_element.get("position"),
                    "name": clean_text(product.get("name")),
                    "description": clean_text(product.get("description")),
                    "brand": clean_text(product.get("brand", {}).get("name") if isinstance(product.get("brand"), dict) else product.get("brand")),
                    "sku": product.get("sku"),
                    "price_ron": parse_price(str(offer.get("price") or offer.get("lowPrice"))),
                    "low_price_ron": parse_price(str(offer.get("lowPrice"))),
                    "high_price_ron": parse_price(str(offer.get("highPrice"))),
                    "currency": offer.get("priceCurrency"),
                    "availability": offer.get("availability"),
                    "image_url": product.get("image"),
                    "product_url": product.get("@id"),
                    "shop_name": SHOP_NAME,
                    "city": CITY,
                    "category_url": page_url,
                })

        for value in jsonld_object.values():
            products.extend(extract_products_from_jsonld_object(value, page_url))
        return products

    for page_number in range(1, max_pages + 1):
        page_url = build_page_url(page_number)
        try:
            html = get_html(page_url)
        except Exception: break
        soup = BeautifulSoup(html, "lxml")
        
        page_products = []
        scripts = soup.find_all("script", attrs={"type": "application/ld+json"})
        for script in scripts:
            try:
                data = json.loads(script.string or script.get_text())
                page_products.extend(extract_products_from_jsonld_object(data, page_url))
            except (json.JSONDecodeError, TypeError): continue

        new_added = 0
        for p in page_products:
            key = p["product_url"] or p.get("sku") or p["name"]
            if key and key not in seen_products:
                seen_products.add(key)
                all_products.append(p)
                new_added += 1
        
        if new_added == 0: break

    return all_products
