UPDATE skills
SET speciality_id = NULL
WHERE speciality_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM specialities s WHERE s.id = skills.speciality_id);

ALTER TABLE skills
    ADD COLUMN parent_id BIGINT;

ALTER TABLE skills
    ADD CONSTRAINT fk_skills_parent FOREIGN KEY (parent_id) REFERENCES skills (id) ON DELETE SET NULL;

ALTER TABLE skills
    ADD CONSTRAINT fk_skills_speciality FOREIGN KEY (speciality_id) REFERENCES specialities (id) ON DELETE SET NULL;

ALTER TABLE skills
    ADD CONSTRAINT ck_skills_parent_not_self CHECK (parent_id IS NULL OR parent_id <> id);

CREATE INDEX idx_skills_parent_id ON skills (parent_id);
