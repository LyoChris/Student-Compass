# models/scraper/product.py

from pydantic import BaseModel, HttpUrl
from typing import Optional


class Product(BaseModel):
    name: str
    description: Optional[str] = None
    brand: Optional[str] = None
    price_ron: Optional[float] = None
    low_price_ron: Optional[float] = None
    high_price_ron: Optional[float] = None
    currency: Optional[str] = "RON"
    image_url: Optional[HttpUrl] = None
    product_url: Optional[HttpUrl] = None
    shop_name: Optional[str] = None
    city: Optional[str] = None
    category_url: Optional[HttpUrl] = None
