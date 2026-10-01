CREATE TABLE recruiters
(
    id                UUID         NOT NULL,
    company_name      VARCHAR(255) NOT NULL,
    first_name        VARCHAR(255),
    last_name         VARCHAR(255),
    username          VARCHAR(64),
    email             VARCHAR(255),
    password_hash     VARCHAR(128),
    created_at        TIMESTAMP WITHOUT TIME ZONE,
    updated_at        TIMESTAMP WITHOUT TIME ZONE,
    phone_number      VARCHAR(16),
    telegram_username VARCHAR(32),
    telegram_user_id  VARCHAR(16),
    CONSTRAINT pk_recruiters PRIMARY KEY (id)
);
