ALTER TABLE recruiters
    ADD CONSTRAINT uc_recruiters_telegram_user UNIQUE (telegram_user_id);

ALTER TABLE recruiters
    ADD CONSTRAINT uc_recruiters_email UNIQUE (email);

ALTER TABLE recruiters
    ADD CONSTRAINT uc_recruiters_username UNIQUE (username);
