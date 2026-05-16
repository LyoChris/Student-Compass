# Student-Compass/ai-microservie/app/modules/scraper/emag_scraper.py

import json
from bs4 import BeautifulSoup
from app.core.scraper.http_client import get_html
from app.core.scraper.text_utils import clean_text, parse_price
from urllib.parse import urljoin

SHOP_NAME = "eMAG"
CITY = "Romania"
BASE_URL = "https://www.emag.ro"

def scrape_emag_category(category_url: str, category_name: str, max_pages: int = 2):
    all_products = []
    seen_products = set()

    def build_page_url(page_number: int):
        if page_number == 1:
            return category_url
        separator = "&" if "?" in category_url else "?"
        return f"{category_url}{separator}p={page_number}"

    def extract_products_from_jsonld_object(jsonld_object, page_url):
        products = []
        if isinstance(jsonld_object, list):
            for item in jsonld_object:
                products.extend(extract_products_from_jsonld_object(item, page_url))
            return products
        if not isinstance(jsonld_object, dict):
            return products

        object_type = jsonld_object.get("@type")
        if object_type == "ItemList":
            item_list_elements = jsonld_object.get("itemListElement", [])
            for list_element in item_list_elements:
                if not isinstance(list_element, dict): continue
                product = list_element.get("item")
                if isinstance(product, dict):
                    products.append(normalize_product(product, page_url, list_element.get("position")))
        elif object_type == "Product":
            products.append(normalize_product(jsonld_object, page_url))

        for value in jsonld_object.values():
            products.extend(extract_products_from_jsonld_object(value, page_url))
        return products

    def normalize_product(product, page_url, position=None):
        offers = product.get("offers", {})
        if isinstance(offers, list) and offers: offers = offers[0]
        
        product_url = product.get("url") or product.get("@id")
        if product_url: product_url = urljoin(BASE_URL, product_url)
        
        image = product.get("image")
        if isinstance(image, list) and image: image = image[0]

        return {
            "category_name": category_name,
            "position": position,
            "name": clean_text(product.get("name")),
            "description": clean_text(product.get("description")),
            "brand": clean_text(product.get("brand", {}).get("name") if isinstance(product.get("brand"), dict) else product.get("brand")),
            "sku": product.get("sku"),
            "price_ron": parse_price(offers.get("price") or offers.get("lowPrice")),
            "low_price_ron": parse_price(offers.get("lowPrice")),
            "high_price_ron": parse_price(offers.get("highPrice")),
            "currency": offers.get("priceCurrency"),
            "availability": offers.get("availability"),
            "image_url": image,
            "product_url": product_url,
            "shop_name": SHOP_NAME,
            "city": CITY,
            "category_url": page_url,
        }

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

        if not page_products:
            cards = soup.select(".card-item, .js-product-data, [data-name], [data-product-id]")
            for card in cards:
                name = card.get("data-name") or card.get("data-title")
                if not name:
                    title_el = card.select_one(".card-v2-title, .product-title, a[href*='/pd/']")
                    if title_el: name = clean_text(title_el.get_text())
                price_el = card.select_one(".product-new-price, .price, [class*='price']")
                price = parse_price(price_el.get_text()) if price_el else None
                link_el = card.select_one("a[href]")
                p_url = urljoin(BASE_URL, link_el.get("href")) if link_el else None
                if not name or price is None: continue
                page_products.append({
                    "category_name": category_name,
                    "name": clean_text(name),
                    "price_ron": price,
                    "product_url": p_url,
                    "sku": card.get("data-product-id"),
                    "shop_name": SHOP_NAME,
                    "city": CITY,
                    "category_url": page_url,
                })

        if not page_products: break
        for p in page_products:
            key = p.get("product_url") or p.get("sku") or p.get("name")
            if key and key not in seen_products:
                seen_products.add(key)
                all_products.append(p)
    return all_products
