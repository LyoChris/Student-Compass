-- Dorms catalog (city-scoped)
CREATE TABLE dorms (
    id      UUID         PRIMARY KEY,
    city_id UUID         NOT NULL,
    name    VARCHAR(120) NOT NULL,
    CONSTRAINT fk_dorms_city FOREIGN KEY (city_id) REFERENCES cities (id),
    CONSTRAINT uq_dorms_city_name UNIQUE (city_id, name)
);

INSERT INTO dorms (id, city_id, name) VALUES
    ('d0000001-0000-0000-0000-000000000000', '11111111-1111-1111-1111-111111111111', 'Camin T1 Titu Maiorescu'),
    ('d0000002-0000-0000-0000-000000000000', '11111111-1111-1111-1111-111111111111', 'Camin T2 Titu Maiorescu'),
    ('d0000003-0000-0000-0000-000000000000', '11111111-1111-1111-1111-111111111111', 'Camin C Tudor Vladimirescu'),
    ('d0000004-0000-0000-0000-000000000000', '22222222-2222-2222-2222-222222222222', 'Camin Avram Iancu'),
    ('d0000005-0000-0000-0000-000000000000', '22222222-2222-2222-2222-222222222222', 'Camin Observator');

-- Optional dorm on student profile
ALTER TABLE student_profiles
    ADD COLUMN dorm_id UUID NULL;

ALTER TABLE student_profiles
    ADD CONSTRAINT fk_student_profiles_dorm FOREIGN KEY (dorm_id) REFERENCES dorms (id);

-- Denormalized seller location snapshot on marketplace listings
ALTER TABLE marketplace_items
    ADD COLUMN seller_city_id    UUID NULL,
    ADD COLUMN seller_faculty_id UUID NULL,
    ADD COLUMN seller_dorm_id    UUID NULL;

CREATE INDEX idx_marketplace_items_seller_loc
    ON marketplace_items (status, seller_city_id, seller_faculty_id);
