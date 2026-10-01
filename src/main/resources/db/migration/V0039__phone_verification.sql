ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS phone_verifications
(
    id                UUID PRIMARY KEY,
    phone_number      VARCHAR(32)  NOT NULL,
    telegram_user_id  VARCHAR(32),
    telegram_chat_id  BIGINT,
    status            VARCHAR(16)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    confirmed_at      TIMESTAMP,
    expires_at        TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_phone_verifications_status ON phone_verifications (status);
CREATE INDEX IF NOT EXISTS idx_phone_verifications_phone ON phone_verifications (phone_number);
