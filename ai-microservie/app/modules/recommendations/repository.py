from sqlalchemy import select
from sqlalchemy.engine import Connection

from app.core.db import (
    catalog_products,
    stores,
    student_fixed_expenses,
    student_profiles,
)
from app.modules.recommendations.schemas import ProductRow, ProfileRow


def load_profile(conn: Connection, user_id: str) -> ProfileRow | None:
    row = conn.execute(
        select(
            student_profiles.c.user_id,
            student_profiles.c.living_area,
            student_profiles.c.eating_habit,
            student_profiles.c.home_package_frequency,
            student_profiles.c.monthly_budget,
            student_profiles.c.dorm_id,
        ).where(student_profiles.c.user_id == user_id)
    ).first()
    if row is None:
        return None

    expenses = conn.execute(
        select(
            student_fixed_expenses.c.expense_name,
            student_fixed_expenses.c.amount,
        ).where(student_fixed_expenses.c.profile_id == user_id)
    ).fetchall()

    return ProfileRow(
        user_id=str(row.user_id),
        living_area=row.living_area,
        eating_habit=row.eating_habit,
        home_package_frequency=row.home_package_frequency,
        monthly_budget=float(row.monthly_budget)
        if row.monthly_budget is not None
        else None,
        dorm_id=str(row.dorm_id) if row.dorm_id is not None else None,
        fixed_expenses=[(e.expense_name, float(e.amount)) for e in expenses],
    )


def load_published_products(conn: Connection) -> list[ProductRow]:
    # INNER JOIN: products whose store is missing are intentionally excluded.
    # Production FK on store_id guarantees this only drops orphaned rows.
    rows = conn.execute(
        select(
            catalog_products.c.id,
            catalog_products.c.name,
            catalog_products.c.price,
            catalog_products.c.category,
            stores.c.name.label("store_name"),
            stores.c.is_partner,
        )
        .select_from(
            catalog_products.join(
                stores, catalog_products.c.store_id == stores.c.id
            )
        )
        .where(catalog_products.c.status == "PUBLISHED")
    ).fetchall()
    return [
        ProductRow(
            product_id=str(r.id),
            name=r.name,
            price=float(r.price),
            category=r.category,
            store_name=r.store_name,
            is_partner=bool(r.is_partner),
        )
        for r in rows
    ]
