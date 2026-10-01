CREATE TABLE vacancies
(
    id                          UUID         NOT NULL,
    recruiter_id                UUID         NOT NULL,
    title                       VARCHAR(255) NOT NULL,
    description                 TEXT,
    company_name                VARCHAR(255),
    city                        VARCHAR(255),
    work_format                 VARCHAR(16),
    employment_type             VARCHAR(16),
    speciality_id               BIGINT,
    status                      VARCHAR(16)  NOT NULL,
    published_from              TIMESTAMP WITHOUT TIME ZONE,
    published_to                TIMESTAMP WITHOUT TIME ZONE,
    slots_count                 INTEGER,
    submitted_for_review_at     TIMESTAMP WITHOUT TIME ZONE,
    moderated_at                TIMESTAMP WITHOUT TIME ZONE,
    moderated_by_username       VARCHAR(64),
    moderation_rejection_reason TEXT,
    created_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_vacancies PRIMARY KEY (id),
    CONSTRAINT fk_vacancies_recruiter FOREIGN KEY (recruiter_id) REFERENCES recruiters (id),
    CONSTRAINT fk_vacancies_speciality FOREIGN KEY (speciality_id) REFERENCES specialities (id)
);

CREATE INDEX idx_vacancies_status_published ON vacancies (status, published_from, published_to);
CREATE INDEX idx_vacancies_recruiter ON vacancies (recruiter_id);
CREATE INDEX idx_vacancies_speciality ON vacancies (speciality_id);

CREATE TABLE vacancy_skills
(
    vacancy_id UUID   NOT NULL,
    skill_id   BIGINT NOT NULL,
    CONSTRAINT pk_vacancy_skills PRIMARY KEY (vacancy_id, skill_id),
    CONSTRAINT fk_vacancy_skills_vacancy FOREIGN KEY (vacancy_id) REFERENCES vacancies (id) ON DELETE CASCADE,
    CONSTRAINT fk_vacancy_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
);

CREATE INDEX idx_vacancy_skills_skill_id ON vacancy_skills (skill_id);

CREATE TABLE vacancy_applications
(
    id               UUID         NOT NULL,
    vacancy_id       UUID         NOT NULL,
    student_id       UUID         NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    cover_letter     TEXT,
    recruiter_comment TEXT,
    rejection_reason TEXT,
    app_chat_id      UUID,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_vacancy_applications PRIMARY KEY (id),
    CONSTRAINT uq_vacancy_applications_vacancy_student UNIQUE (vacancy_id, student_id),
    CONSTRAINT fk_vacancy_applications_vacancy FOREIGN KEY (vacancy_id) REFERENCES vacancies (id) ON DELETE CASCADE,
    CONSTRAINT fk_vacancy_applications_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_vacancy_applications_chat FOREIGN KEY (app_chat_id) REFERENCES chats (id)
);

CREATE INDEX idx_vacancy_applications_vacancy ON vacancy_applications (vacancy_id);
CREATE INDEX idx_vacancy_applications_student ON vacancy_applications (student_id);
