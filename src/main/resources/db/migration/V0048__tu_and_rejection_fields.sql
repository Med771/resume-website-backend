ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS student_tu_confirmed_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS recruiter_tu_confirmed_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS rejection_reason_code VARCHAR(64);

ALTER TABLE requests
    ADD COLUMN IF NOT EXISTS rejection_comment TEXT;

ALTER TABLE vacancy_applications
    ADD COLUMN IF NOT EXISTS student_tu_confirmed_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE vacancy_applications
    ADD COLUMN IF NOT EXISTS recruiter_tu_confirmed_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE vacancy_applications
    ADD COLUMN IF NOT EXISTS rejection_reason_code VARCHAR(64);

ALTER TABLE vacancy_applications
    ADD COLUMN IF NOT EXISTS rejection_comment TEXT;
