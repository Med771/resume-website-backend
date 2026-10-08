ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_otp_hash VARCHAR(128);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_reset_otp_expires_at TIMESTAMP;
