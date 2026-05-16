CREATE TABLE cities (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE faculties (
    id UUID PRIMARY KEY,
    city_id UUID NOT NULL,
    name VARCHAR(180) NOT NULL,
    CONSTRAINT fk_faculties_city FOREIGN KEY (city_id) REFERENCES cities (id),
    CONSTRAINT uq_faculties_city_name UNIQUE (city_id, name)
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(120) NOT NULL,
    last_name VARCHAR(120) NOT NULL,
    role VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    city_id UUID NOT NULL,
    faculty_id UUID NOT NULL,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_city FOREIGN KEY (city_id) REFERENCES cities (id),
    CONSTRAINT fk_users_faculty FOREIGN KEY (faculty_id) REFERENCES faculties (id)
);

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_city_faculty ON users (city_id, faculty_id);

CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);

CREATE TABLE revoked_access_token (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO cities (id, name) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Iasi'),
    ('22222222-2222-2222-2222-222222222222', 'Cluj-Napoca');

INSERT INTO faculties (id, city_id, name) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'Facultatea de Informatica'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222', 'Facultatea de Medicina');
