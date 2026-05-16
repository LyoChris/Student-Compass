CREATE TABLE marketplace_items (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    category VARCHAR(32) NOT NULL,
    item_condition VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    is_boosted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_marketplace_title_not_blank
        CHECK (length(btrim(title)) > 0),
    CONSTRAINT chk_marketplace_description_not_blank
        CHECK (length(btrim(description)) > 0),
    CONSTRAINT chk_marketplace_contact_phone_not_blank
        CHECK (length(btrim(contact_phone)) > 0),
    CONSTRAINT chk_marketplace_price_positive
        CHECK (price >= 0),
    CONSTRAINT chk_marketplace_category_enum
        CHECK (category IN ('BOOKS_NOTES', 'ELECTRONICS', 'DORM_APPLIANCES', 'CLOTHING', 'OTHER')),
    CONSTRAINT chk_marketplace_condition_enum
        CHECK (item_condition IN ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR')),
    CONSTRAINT chk_marketplace_status_enum
        CHECK (status IN ('ACTIVE', 'RESERVED', 'SOLD', 'INACTIVE'))
);

CREATE TABLE student_marketplace_tags (
    item_id UUID NOT NULL,
    position INTEGER NOT NULL,
    tag VARCHAR(100) NOT NULL,

    CONSTRAINT pk_student_marketplace_tags
        PRIMARY KEY (item_id, position),
    CONSTRAINT fk_student_marketplace_tags_item
        FOREIGN KEY (item_id) REFERENCES marketplace_items (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_student_marketplace_tag_not_blank
        CHECK (length(btrim(tag)) > 0)
);

CREATE TABLE student_marketplace_images (
    item_id UUID NOT NULL,
    position INTEGER NOT NULL,
    image_url VARCHAR(512) NOT NULL,

    CONSTRAINT pk_student_marketplace_images
        PRIMARY KEY (item_id, position),
    CONSTRAINT fk_student_marketplace_images_item
        FOREIGN KEY (item_id) REFERENCES marketplace_items (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_student_marketplace_image_url_not_blank
        CHECK (length(btrim(image_url)) > 0),
    CONSTRAINT chk_student_marketplace_image_url_http
        CHECK (image_url LIKE 'http://%' OR image_url LIKE 'https://%')
);

CREATE INDEX idx_marketplace_items_status_boosted_created
    ON marketplace_items (status, is_boosted DESC, created_at DESC);

CREATE INDEX idx_marketplace_items_status_category
    ON marketplace_items (status, category);

CREATE INDEX idx_marketplace_items_status_condition
    ON marketplace_items (status, item_condition);

CREATE INDEX idx_marketplace_items_price
    ON marketplace_items (price);

CREATE INDEX idx_student_marketplace_tags_item
    ON student_marketplace_tags (item_id);

CREATE INDEX idx_student_marketplace_images_item
    ON student_marketplace_images (item_id);
