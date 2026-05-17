import pytest
from fastapi.testclient import TestClient

from app.main import create_app


@pytest.fixture
def app():
    return create_app()


@pytest.fixture
def client(app):
    return TestClient(app)


from app.core.config import settings

SECRET_HEADER = {"X-Internal-Secret": settings.ai_service_shared_secret}
