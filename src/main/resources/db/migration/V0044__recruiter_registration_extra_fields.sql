ALTER TABLE recruiter_registration_requests
    ADD COLUMN IF NOT EXISTS middle_name VARCHAR(255);

ALTER TABLE recruiter_registration_requests
    ADD COLUMN IF NOT EXISTS city VARCHAR(255);

ALTER TABLE recruiter_registration_requests
    ADD COLUMN IF NOT EXISTS marketing_consent BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE recruiter_registration_requests
    ADD COLUMN IF NOT EXISTS phone_verification_id UUID;
