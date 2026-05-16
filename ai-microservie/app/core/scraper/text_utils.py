# core/scraper/text_utils.py

import re

def clean_text(value: str) -> str:
    """
    Remove extra whitespace, newline characters, and normalize spaces.
    """
    if value is None:
        return ""
    value = str(value)
    value = re.sub(r"\s+", " ", value)
    return value.strip()


def parse_price(value: str) -> float | None:
    """
    Parse price string to float. Handles formats like '259,99', '259.99', '1.259,99', '1,259.99'.
    Returns None if parsing fails.
    """
    if not value:
        return None
    
    value = str(value).strip()
    # Find all number-like parts
    # We want to identify the decimal part if it exists.
    # Usually it's the last 2 digits after a , or .
    
    # Remove all but digits, comma and dot
    value = re.sub(r"[^\d.,]", "", value)
    
    if not value:
        return None

    # If there are both dots and commas, the last one is likely the decimal separator
    if "." in value and "," in value:
        dot_idx = value.rfind(".")
        comma_idx = value.rfind(",")
        if dot_idx > comma_idx:
            # Dot is decimal, remove commas
            value = value.replace(",", "").replace(".", ".")
        else:
            # Comma is decimal, remove dots, then replace comma with dot
            value = value.replace(".", "").replace(",", ".")
    elif "," in value:
        # Only commas. Could be thousands or decimal.
        # If it's like XXX,YY it's likely decimal.
        # If it's like X,XXX,XXX it's thousands.
        parts = value.split(",")
        if len(parts[-1]) <= 2:
            # Likely decimal
            value = "".join(parts[:-1]) + "." + parts[-1]
        else:
            # Likely thousands
            value = "".join(parts)
    elif "." in value:
        # Only dots. Similar logic.
        parts = value.split(".")
        if len(parts[-1]) <= 2:
            # Likely decimal
            value = "".join(parts[:-1]) + "." + parts[-1]
        else:
            # Likely thousands
            value = "".join(parts)
            
    try:
        return float(value)
    except ValueError:
        return None


def parse_old_and_new_prices(text: str) -> tuple[float | None, float | None]:
    """
    Extract old price and final price from a string like
    '259.00 lei /bucată Super Preț 229.00 lei /bucată'
    """
    if not text:
        return None, None

    prices = re.findall(r"(\d+(?:[.,]\d+)?)", text)
    if not prices:
        return None, None

    if len(prices) == 1:
        return None, float(prices[0].replace(",", "."))
    else:
        return float(prices[0].replace(",", ".")), float(prices[1].replace(",", "."))
