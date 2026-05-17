-- ============================================================
-- V7 — StuFi Radar deals map + global Trust Score system
-- ============================================================

-- 1. Trust score column on users (default 100, never negative)
ALTER TABLE users
    ADD COLUMN trust_score INTEGER NOT NULL DEFAULT 100;

ALTER TABLE users
    ADD CONSTRAINT chk_users_trust_score_non_negative CHECK (trust_score >= 0);

-- 2. Radar deals
CREATE TABLE radar_deals (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(120)    NOT NULL,
    description     VARCHAR(500),
    category        VARCHAR(30)     NOT NULL,
    latitude        DECIMAL(9, 6)   NOT NULL,
    longitude       DECIMAL(9, 6)   NOT NULL,
    expires_at      TIMESTAMP       NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_radar_deals_latitude  CHECK (latitude  BETWEEN -90  AND  90),
    CONSTRAINT chk_radar_deals_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_radar_deals_status    CHECK (status IN ('ACTIVE', 'EXPIRED'))
);

CREATE INDEX idx_radar_deals_user_id   ON radar_deals(user_id);
CREATE INDEX idx_radar_deals_status    ON radar_deals(status);
CREATE INDEX idx_radar_deals_expires_at ON radar_deals(expires_at);

-- 3. Radar votes (composite PK — one vote per (user, deal))
CREATE TABLE radar_votes (
    user_id     UUID        NOT NULL REFERENCES users(id)       ON DELETE CASCADE,
    deal_id     UUID        NOT NULL REFERENCES radar_deals(id) ON DELETE CASCADE,
    vote_type   VARCHAR(10) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_radar_votes        PRIMARY KEY (user_id, deal_id),
    CONSTRAINT chk_radar_votes_type  CHECK (vote_type IN ('UPVOTE', 'DOWNVOTE'))
);

CREATE INDEX idx_radar_votes_deal_id ON radar_votes(deal_id);

-- 4. Radar comments
CREATE TABLE radar_comments (
    id          UUID            PRIMARY KEY,
    deal_id     UUID            NOT NULL REFERENCES radar_deals(id) ON DELETE CASCADE,
    user_id     UUID            NOT NULL REFERENCES users(id)       ON DELETE CASCADE,
    content     VARCHAR(300)    NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_radar_comments_deal_id ON radar_comments(deal_id);
