from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "Student Compass API"
    api_prefix: str = ""


settings = Settings()
