from collections.abc import Iterator

from sqlalchemy import (
    Boolean,
    Column,
    MetaData,
    Numeric,
    String,
    Table,
    create_engine,
)
from sqlalchemy.engine import Connection, Engine

from app.core.config import settings

metadata = MetaData()

# Lightweight read models — ONLY the columns the AI service SELECTs.
# The backend owns these tables and their Flyway migrations.

student_profiles = Table(
    "student_profiles",
    metadata,
    Column("user_id", String, primary_key=True),
    Column("living_area", String),
    Column("eating_habit", String),
    Column("home_package_frequency", String),
    Column("monthly_budget", Numeric),
    Column("dorm_id", String),
)

student_fixed_expenses = Table(
    "student_fixed_expenses",
    metadata,
    Column("id", String, primary_key=True),
    Column("profile_id", String),
    Column("expense_name", String),
    Column("amount", Numeric),
)

stores = Table(
    "stores",
    metadata,
    Column("id", String, primary_key=True),
    Column("name", String),
    Column("is_partner", Boolean),
)

catalog_products = Table(
    "catalog_products",
    metadata,
    Column("id", String, primary_key=True),
    Column("store_id", String),
    Column("name", String),
    Column("price", Numeric),
    Column("category", String),
    Column("unit", String),
    Column("image_url", String),
    Column("source_url", String),
    Column("status", String),
)

_engine: Engine | None = None


def get_engine() -> Engine:
    global _engine
    if _engine is None:
        _engine = create_engine(settings.database_url, future=True)
    return _engine


def get_db() -> Iterator[Connection]:
    """FastAPI dependency: a read-only connection per request."""
    with get_engine().connect() as conn:
        yield conn
