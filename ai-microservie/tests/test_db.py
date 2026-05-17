from sqlalchemy import create_engine, insert
from sqlalchemy.pool import StaticPool

from app.core.db import metadata, catalog_products, stores


def test_read_models_create_and_select():
    engine = create_engine(
        "sqlite+pysqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
        future=True,
    )
    metadata.create_all(engine)
    with engine.begin() as conn:
        conn.execute(insert(stores).values(id="s1", name="Acme", is_partner=True))
        conn.execute(
            insert(catalog_products).values(
                id="p1", store_id="s1", name="Rice 1kg", price=9.5,
                category="FOOD", unit="kg", image_url=None,
                source_url=None, status="PUBLISHED",
            )
        )
    with engine.connect() as conn:
        rows = conn.execute(catalog_products.select()).fetchall()
    assert rows[0].name == "Rice 1kg"
    assert rows[0].status == "PUBLISHED"
