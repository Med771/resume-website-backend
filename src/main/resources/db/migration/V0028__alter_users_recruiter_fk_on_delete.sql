-- При удалении рекрутера снимаем привязку у пользователя (профиль заявок можно заново заполнить)
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS fk_users_recruiter;

ALTER TABLE users
    ADD CONSTRAINT fk_users_recruiter
        FOREIGN KEY (recruiter_id) REFERENCES recruiters (id) ON DELETE SET NULL;
