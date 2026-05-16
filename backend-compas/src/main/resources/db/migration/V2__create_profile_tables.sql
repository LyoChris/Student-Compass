-- Student financial profile (one row per user, user_id IS the PK)
CREATE TABLE student_profiles (
    user_id                 UUID         NOT NULL,
    living_area             VARCHAR(30)  NOT NULL,
    eating_habit            VARCHAR(30)  NOT NULL,
    home_package_frequency  VARCHAR(30)  NOT NULL,
    monthly_budget          DECIMAL(12, 2) NOT NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_student_profiles
        PRIMARY KEY (user_id),
    CONSTRAINT fk_student_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_monthly_budget_positive
        CHECK (monthly_budget > 0)
);

-- Fixed monthly expenses belonging to a profile
CREATE TABLE student_fixed_expenses (
    profile_id    UUID           NOT NULL,
    expense_name  VARCHAR(100)   NOT NULL,
    amount        DECIMAL(12, 2) NOT NULL,

    CONSTRAINT fk_fixed_expenses_profile
        FOREIGN KEY (profile_id) REFERENCES student_profiles (user_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_fixed_expense_amount_positive
        CHECK (amount > 0)
);
