-- Навыки: убрать student_id с таблицы skills (одна строка навыка «переезжала» между студентами).
-- Связь студент ↔ навык — многие-ко-многим через student_skills.

CREATE TABLE student_skills
(
    student_id UUID   NOT NULL,
    skill_id   BIGINT NOT NULL,
    CONSTRAINT pk_student_skills PRIMARY KEY (student_id, skill_id),
    CONSTRAINT fk_student_skills_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
);

CREATE INDEX idx_student_skills_skill_id ON student_skills (skill_id);

INSERT INTO student_skills (student_id, skill_id)
SELECT student_id, id
FROM skills
WHERE student_id IS NOT NULL;

ALTER TABLE skills
    DROP CONSTRAINT IF EXISTS fk_skills_on_student;

ALTER TABLE skills
    DROP COLUMN student_id;
