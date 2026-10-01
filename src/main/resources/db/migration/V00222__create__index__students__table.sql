CREATE INDEX idx_students_fullname_trgm
    ON students
        USING gin (lower(first_name || ' ' || last_name) gin_trgm_ops);

CREATE INDEX idx_students_bio_trgm
    ON students
        USING gin (lower(bio) gin_trgm_ops);