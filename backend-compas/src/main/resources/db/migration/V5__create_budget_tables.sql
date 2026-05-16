CREATE TABLE budget_plans (
    id             UUID           PRIMARY KEY,
    user_id        UUID           NOT NULL UNIQUE,
    monthly_budget DECIMAL(12, 2) NOT NULL,
    fixed_total    DECIMAL(12, 2) NOT NULL,
    disposable     DECIMAL(12, 2) NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_budget_plans_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_budget_disposable_non_negative CHECK (disposable >= 0)
);

CREATE TABLE budget_plan_categories (
    plan_id  UUID           NOT NULL,
    category VARCHAR(32)    NOT NULL,
    amount   DECIMAL(12, 2) NOT NULL,
    CONSTRAINT pk_budget_plan_categories PRIMARY KEY (plan_id, category),
    CONSTRAINT fk_budget_categories_plan FOREIGN KEY (plan_id) REFERENCES budget_plans (id) ON DELETE CASCADE
);
