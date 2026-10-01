ALTER TABLE requests
    ADD COLUMN student_response_text TEXT,
    ADD COLUMN has_recruiter_message BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN has_student_message BOOLEAN NOT NULL DEFAULT FALSE;
