import pytest
from fastapi.testclient import TestClient

from app.core.config import settings
from app.main import create_app

SECRET_HEADER = {"X-Internal-Secret": settings.ai_service_shared_secret}


@pytest.fixture
def app():
    return create_app()


@pytest.fixture
def client(app):
    return TestClient(app)
