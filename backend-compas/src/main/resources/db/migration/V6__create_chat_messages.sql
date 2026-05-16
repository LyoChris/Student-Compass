CREATE TABLE chat_messages (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL,
    role       VARCHAR(16) NOT NULL,
    content    TEXT        NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_chat_messages_role CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX idx_chat_messages_user_created
    ON chat_messages (user_id, created_at DESC);
