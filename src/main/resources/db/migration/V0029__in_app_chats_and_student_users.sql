-- In-app чаты (один на пару рекрутер–студент), сообщения, прочитанность; привязка студента к users; заявки без Telegram-полей

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE chats
(
    id                 UUID         NOT NULL,
    recruiter_id       UUID         NOT NULL,
    student_id       UUID         NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE,
    updated_at       TIMESTAMP WITHOUT TIME ZONE,
    last_activity_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_chats PRIMARY KEY (id),
    CONSTRAINT uq_chats_recruiter_student UNIQUE (recruiter_id, student_id),
    CONSTRAINT fk_chats_recruiter FOREIGN KEY (recruiter_id) REFERENCES recruiters (id),
    CONSTRAINT fk_chats_student FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE TABLE chat_messages
(
    id                      UUID                        NOT NULL,
    chat_id                 UUID                        NOT NULL,
    author_user_id          UUID,
    message_kind            VARCHAR(16)                 NOT NULL,
    system_event            VARCHAR(64),
    body                    TEXT,
    attachment_storage_name VARCHAR(512),
    created_at              TIMESTAMP WITHOUT TIME ZONE,
    updated_at              TIMESTAMP WITHOUT TIME ZONE,
    edited_at               TIMESTAMP WITHOUT TIME ZONE,
    deleted_at              TIMESTAMP WITHOUT TIME ZONE,
    deleted_by_admin        BOOLEAN                     NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_chat_messages PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_chat FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_author FOREIGN KEY (author_user_id) REFERENCES users (id)
);

CREATE INDEX idx_chat_messages_chat_created ON chat_messages (chat_id, created_at);

CREATE TABLE chat_read_states
(
    chat_id               UUID NOT NULL,
    user_id               UUID NOT NULL,
    last_read_message_id  UUID,
    last_read_at          TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_chat_read_states PRIMARY KEY (chat_id, user_id),
    CONSTRAINT fk_read_chat FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE,
    CONSTRAINT fk_read_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Заявки: связь с приложением-чатом, удаление полей Telegram/метрик
ALTER TABLE requests
    ADD COLUMN app_chat_id UUID;

INSERT INTO chats (id, recruiter_id, student_id, created_at, updated_at, last_activity_at)
SELECT gen_random_uuid(),
       r.recruiter_id,
       r.student_id,
       MIN(r.created_at),
       MAX(r.updated_at),
       COALESCE(MAX(r.updated_at), MAX(r.created_at), NOW())
FROM requests r
GROUP BY r.recruiter_id, r.student_id;

UPDATE requests req
SET app_chat_id = c.id
FROM chats c
WHERE req.recruiter_id = c.recruiter_id
  AND req.student_id = c.student_id;

ALTER TABLE requests
    ALTER COLUMN app_chat_id SET NOT NULL;

ALTER TABLE requests
    ADD CONSTRAINT fk_requests_app_chat FOREIGN KEY (app_chat_id) REFERENCES chats (id);

ALTER TABLE requests
    DROP COLUMN IF EXISTS chat_id;

ALTER TABLE requests
    DROP COLUMN IF EXISTS chat_title;

ALTER TABLE requests
    DROP COLUMN IF EXISTS chat_url;

ALTER TABLE requests
    DROP COLUMN IF EXISTS has_recruiter_message;

ALTER TABLE requests
    DROP COLUMN IF EXISTS has_student_message;

-- Пользователь–студент (1:1)
ALTER TABLE users
    ADD COLUMN student_id UUID;

ALTER TABLE users
    ADD CONSTRAINT uq_users_student UNIQUE (student_id);

ALTER TABLE users
    ADD CONSTRAINT fk_users_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE SET NULL;
