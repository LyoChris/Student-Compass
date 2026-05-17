# Student-Compass/ai-microservie/app/modules/scraper/dedeman_scraper.py

from bs4 import BeautifulSoup
from app.core.scraper.http_client import get_html
from app.core.scraper.text_utils import clean_text, parse_price
from urllib.parse import urljoin

SHOP_NAME = "Dedeman"
CITY = "Romania"
BASE_URL = "https://www.dedeman.ro"

def scrape_dedeman_category(category_url: str, category_name: str, max_pages: int = 2):
    all_products = []
    seen_products = set()

    def build_page_url(page_number: int):
        if page_number == 1: return category_url
        return f"{category_url}?p={page_number}"

    def extract_price_from_card(card, selector):
        price_element = card.select_one(selector)
        if price_element is None: return None
        data_price_amount = price_element.get("data-price-amount")
        if data_price_amount: return parse_price(data_price_amount)
        return parse_price(clean_text(price_element.get_text()))

    for page_number in range(1, max_pages + 1):
        page_url = build_page_url(page_number)
        try:
            html = get_html(page_url)
        except Exception: break
        soup = BeautifulSoup(html, "lxml")
        product_cards = soup.select("div.item.product.product-item")

        new_added = 0
        for card in product_cards:
            product_id = card.get("data-product-id")
            title_el = card.select_one("a.product-item-link")
            if not title_el: continue
            name = clean_text(title_el.get_text())
            product_url = urljoin(BASE_URL, title_el.get("href"))
            current_price = extract_price_from_card(card, ".price-wrapper.final-price, [data-price-type='finalPrice']")
            old_price = extract_price_from_card(card, ".price-wrapper.old-price, [data-price-type='oldPrice']")
            image_el = card.select_one("img.product-image-photo")
            image_url = image_el.get("src") if image_el else None

            if not name or current_price is None: continue
            key = product_url or product_id or name
            if key in seen_products: continue
            seen_products.add(key)

            all_products.append({
                "category_name": category_name,
                "sku": product_id,
                "name": name,
                "price_ron": current_price,
                "high_price_ron": old_price,
                "image_url": image_url,
                "product_url": product_url,
                "shop_name": SHOP_NAME,
                "city": CITY,
                "category_url": page_url,
                "currency": "RON"
            })
            new_added += 1

        if new_added == 0: break

    return all_products
