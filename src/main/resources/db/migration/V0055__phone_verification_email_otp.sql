ALTER TABLE phone_verifications
    ADD COLUMN IF NOT EXISTS email VARCHAR(255);

ALTER TABLE phone_verifications
    ADD COLUMN IF NOT EXISTS otp_code_hash VARCHAR(255);
