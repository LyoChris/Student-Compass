# core/scraper/http_client.py

import requests
import time
from typing import Optional

DEFAULT_HEADERS = {
    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64; rv:126.0) Gecko/20100101 Firefox/126.0",
    "Accept-Language": "ro-RO,ro;q=0.9,en-US;q=0.8,en;q=0.7",
}

def get_html(
    url: str,
    headers: Optional[dict] = None,
    timeout: int = 30,
    retries: int = 3,
    delay: float = 1.0
) -> str:
    headers = headers or DEFAULT_HEADERS

    for attempt in range(1, retries + 1):
        try:
            response = requests.get(url, headers=headers, timeout=timeout)
            response.raise_for_status()
            return response.text
        except requests.RequestException as e:
            if attempt == retries:
                raise
            time.sleep(delay)
