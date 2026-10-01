-- Связь проект ленты ↔ студент (многие-ко-многим).

CREATE TABLE site_project_students
(
    site_project_id UUID NOT NULL,
    student_id      UUID NOT NULL,
    CONSTRAINT pk_site_project_students PRIMARY KEY (site_project_id, student_id),
    CONSTRAINT fk_site_project_students_project FOREIGN KEY (site_project_id) REFERENCES site_projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_site_project_students_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE
);

CREATE INDEX idx_site_project_students_student ON site_project_students (student_id);
