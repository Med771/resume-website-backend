-- Заявки на саморегистрацию работодателя (рекрутера); до одобрения аккаунт в users не создаётся

CREATE TABLE recruiter_registration_requests
(
    id                      UUID                        NOT NULL,
    username                VARCHAR(64)                 NOT NULL,
    password_hash           VARCHAR(255)                NOT NULL,
    name                    VARCHAR(255),
    company_name            VARCHAR(255)                NOT NULL,
    first_name              VARCHAR(255),
    last_name               VARCHAR(255),
    email                   VARCHAR(255),
    phone_number            VARCHAR(32),
    telegram_username       VARCHAR(32),
    status                  VARCHAR(32)                 NOT NULL,
    reject_reason           TEXT,
    processed_at            TIMESTAMP WITHOUT TIME ZONE,
    processed_by_username   VARCHAR(64),
    approved_user_id        UUID,
    created_at              TIMESTAMP WITHOUT TIME ZONE,
    updated_at              TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_recruiter_registration_requests PRIMARY KEY (id),
    CONSTRAINT fk_rrr_approved_user FOREIGN KEY (approved_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_recruiter_reg_username_pending
    ON recruiter_registration_requests (lower(username))
    WHERE status = 'PENDING';

CREATE INDEX idx_recruiter_reg_status_created ON recruiter_registration_requests (status, created_at DESC);
