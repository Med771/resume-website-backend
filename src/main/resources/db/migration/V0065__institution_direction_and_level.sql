ALTER TABLE institution
    ADD COLUMN IF NOT EXISTS direction VARCHAR(255);

ALTER TABLE institution
    ADD COLUMN IF NOT EXISTS education_level VARCHAR(255);
