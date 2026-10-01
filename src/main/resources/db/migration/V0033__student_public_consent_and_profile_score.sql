ALTER TABLE students
    ADD COLUMN IF NOT EXISTS public_profile_consent BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS profile_text_score INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN students.public_profile_consent IS 'Разрешение показывать карточку анонимам (без входа)';
COMMENT ON COLUMN students.profile_text_score IS 'Денормализованная сумма длин текстовых полей для ранжирования';

UPDATE students
SET profile_text_score = COALESCE(LENGTH(bio), 0)
    + COALESCE(LENGTH(city), 0)
    + COALESCE(LENGTH(hh_link), 0)
    + COALESCE(LENGTH(first_name), 0)
    + COALESCE(LENGTH(last_name), 0);

CREATE INDEX IF NOT EXISTS idx_students_public_profile
    ON students (public_profile_consent, course)
    WHERE public_profile_consent = true;
