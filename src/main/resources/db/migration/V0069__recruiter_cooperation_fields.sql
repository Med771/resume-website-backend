ALTER TABLE recruiters
    ADD COLUMN IF NOT EXISTS cooperation_format VARCHAR(32);

ALTER TABLE recruiters
    ADD COLUMN IF NOT EXISTS ready_to_request_students BOOLEAN;
