ALTER TABLE skills
    ADD CONSTRAINT uc_skills_name UNIQUE (name);

ALTER TABLE skills
    ADD CONSTRAINT FK_SKILLS_ON_STUDENT FOREIGN KEY (student_id) REFERENCES students (id);
