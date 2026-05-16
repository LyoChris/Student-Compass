-- =============================================================================
-- V5 : Dynamic monthly budget planner
-- Replaces the old static budget_plans + budget_plan_categories schema.
-- Full DB wipe assumed — no migration guards needed.
-- =============================================================================

-- Per-user, per-month budget envelope
CREATE TABLE monthly_budgets (
    id              UUID           PRIMARY KEY,
    user_id         UUID           NOT NULL,
    month           INTEGER       NOT NULL,
    year            INTEGER       NOT NULL,
    total_income    DECIMAL(12, 2) NOT NULL,
    rollover_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_monthly_budgets_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_monthly_budgets_user_month_year
        UNIQUE (user_id, month, year),
    CONSTRAINT chk_monthly_budgets_month
        CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_monthly_budgets_year
        CHECK (year >= 2020),
    CONSTRAINT chk_monthly_budgets_income_non_negative
        CHECK (total_income >= 0),
    CONSTRAINT chk_monthly_budgets_rollover_non_negative
        CHECK (rollover_amount >= 0)
);

-- Flexible named spending categories inside a monthly budget
CREATE TABLE budget_categories (
    id               UUID           PRIMARY KEY,
    budget_id        UUID           NOT NULL,
    name             VARCHAR(50)    NOT NULL,
    allocated_amount DECIMAL(12, 2) NOT NULL,
    spent_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_budget_categories_budget
        FOREIGN KEY (budget_id) REFERENCES monthly_budgets (id) ON DELETE CASCADE,
    CONSTRAINT uq_budget_category_name
        UNIQUE (budget_id, name),
    CONSTRAINT chk_budget_categories_allocated_non_negative
        CHECK (allocated_amount >= 0),
    CONSTRAINT chk_budget_categories_spent_non_negative
        CHECK (spent_amount >= 0)
);

-- Manual spending entries tied to a single category
CREATE TABLE transactions (
    id               UUID           PRIMARY KEY,
    user_id          UUID           NOT NULL,
    category_id      UUID           NOT NULL,
    amount           DECIMAL(12, 2) NOT NULL,
    description      VARCHAR(255),
    transaction_date TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category
        FOREIGN KEY (category_id) REFERENCES budget_categories (id) ON DELETE CASCADE,
    CONSTRAINT chk_transactions_amount_positive
        CHECK (amount > 0)
);

CREATE INDEX idx_transactions_user_date
    ON transactions (user_id, transaction_date DESC);

CREATE INDEX idx_budget_categories_budget
    ON budget_categories (budget_id);
