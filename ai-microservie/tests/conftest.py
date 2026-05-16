import pytest
from fastapi.testclient import TestClient

from app.main import create_app


@pytest.fixture
def app():
    return create_app()


@pytest.fixture
def client(app):
    return TestClient(app)


SECRET_HEADER = {"X-Internal-Secret": "dev-internal-secret"}
