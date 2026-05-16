from typing import Dict
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "Student Compass API"
    api_prefix: str = ""

    # Storage Settings
    DATA_STORAGE_PATH: str = "data"
    CATEGORY_TO_FOLDER_MAP: Dict[str, str] = {
        "Congelate": "groceries",
        "Scaune birou": "furniture",
        "Masini spalat": "appliances",
        "Igienă": "hygiene",
        "Băuturi": "groceries",
    }


settings = Settings()
