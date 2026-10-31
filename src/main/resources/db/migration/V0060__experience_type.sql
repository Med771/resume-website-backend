ALTER TABLE experiences
    ADD COLUMN IF NOT EXISTS experience_type VARCHAR(32) NOT NULL DEFAULT 'WORK';

UPDATE experiences
SET experience_type = 'WORK'
WHERE experience_type IS NULL OR TRIM(experience_type) = '';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_experiences_experience_type'
    ) THEN
        ALTER TABLE experiences ADD CONSTRAINT chk_experiences_experience_type
            CHECK (experience_type IN ('WORK', 'PROJECT'));
    END IF;
END $$;
