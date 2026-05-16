from sqlalchemy import create_engine, insert
from sqlalchemy.pool import StaticPool

from app.core.db import (
    metadata,
    student_profiles,
    student_fixed_expenses,
    stores,
    catalog_products,
)
from app.modules.recommendations.repository import (
    load_profile,
    load_published_products,
)


def _engine():
    e = create_engine(
        "sqlite+pysqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
        future=True,
    )
    metadata.create_all(e)
    return e


def test_load_profile_returns_none_when_missing():
    e = _engine()
    with e.connect() as conn:
        assert load_profile(conn, "u-missing") is None


def test_load_profile_and_products():
    e = _engine()
    with e.begin() as conn:
        conn.execute(insert(student_profiles).values(
            user_id="u1", living_area="DORMITORY", eating_habit="COOKING",
            home_package_frequency="MONTHLY", monthly_budget=1500, dorm_id=None,
        ))
        conn.execute(insert(student_fixed_expenses).values(
            id="f1", profile_id="u1", expense_name="Gym", amount=80,
        ))
        conn.execute(insert(stores).values(id="s1", name="Acme", is_partner=True))
        conn.execute(insert(catalog_products).values(
            id="p1", store_id="s1", name="Rice", price=9.5, category="FOOD",
            unit="kg", image_url=None, source_url=None, status="PUBLISHED",
        ))
        conn.execute(insert(catalog_products).values(
            id="p2", store_id="s1", name="Draft item", price=1, category="FOOD",
            unit=None, image_url=None, source_url=None, status="DRAFT",
        ))
    with e.connect() as conn:
        prof = load_profile(conn, "u1")
        prods = load_published_products(conn)

    assert prof.living_area == "DORMITORY"
    assert prof.fixed_expenses == [("Gym", 80.0)]
    assert [p.product_id for p in prods] == ["p1"]      # DRAFT excluded
    assert prods[0].store_name == "Acme"
    assert prods[0].is_partner is True
