ALTER TABLE institution
    ADD COLUMN IF NOT EXISTS education_type VARCHAR(32) NOT NULL DEFAULT 'STATE';

UPDATE institution
SET education_type = 'STATE'
WHERE education_type IS NULL OR TRIM(education_type) = '';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_institution_education_type'
    ) THEN
        ALTER TABLE institution ADD CONSTRAINT chk_institution_education_type
            CHECK (education_type IN ('STATE', 'COURSE'));
    END IF;
END $$;
