ALTER TABLE students ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(64);
ALTER TABLE students ADD COLUMN IF NOT EXISTS rejection_comment TEXT;
