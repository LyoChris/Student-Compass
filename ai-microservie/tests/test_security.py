from tests.conftest import SECRET_HEADER


def test_health_is_public(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}


def test_guarded_route_requires_secret(client):
    # /api/v1/_guard_probe is a guard-only probe mounted for tests below.
    resp = client.get("/api/v1/_guard_probe")
    assert resp.status_code == 401


def test_guarded_route_accepts_correct_secret(client):
    resp = client.get("/api/v1/_guard_probe", headers=SECRET_HEADER)
    assert resp.status_code == 200
