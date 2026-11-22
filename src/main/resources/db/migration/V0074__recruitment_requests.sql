CREATE TABLE recruitment_requests
(
    id                         UUID PRIMARY KEY,
    recruiter_id               UUID,
    company_name               VARCHAR(255),
    tasks                      TEXT        NOT NULL,
    qualification_requirements TEXT        NOT NULL,
    work_conditions            TEXT        NOT NULL,
    student_count              INTEGER     NOT NULL,
    cooperation_format         VARCHAR(32),
    ready_to_request_students  BOOLEAN,
    schedule_note              TEXT,
    extra_info                 TEXT,
    published_from             TIMESTAMP,
    published_to               TIMESTAMP,
    status                     VARCHAR(32) NOT NULL,
    created_at                 TIMESTAMP,
    updated_at                 TIMESTAMP,
    CONSTRAINT fk_recruitment_requests_recruiter FOREIGN KEY (recruiter_id) REFERENCES recruiters (id) ON DELETE CASCADE,
    CONSTRAINT ck_recruitment_requests_student_count CHECK (student_count >= 1)
);

CREATE INDEX idx_recruitment_requests_recruiter ON recruitment_requests (recruiter_id);

CREATE TABLE recruitment_request_specialities
(
    recruitment_request_id UUID   NOT NULL,
    speciality_id          BIGINT NOT NULL,
    CONSTRAINT pk_recruitment_request_specialities PRIMARY KEY (recruitment_request_id, speciality_id),
    CONSTRAINT fk_rr_specialities_request FOREIGN KEY (recruitment_request_id) REFERENCES recruitment_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_specialities_speciality FOREIGN KEY (speciality_id) REFERENCES specialities (id) ON DELETE CASCADE
);

CREATE INDEX idx_rr_specialities_speciality ON recruitment_request_specialities (speciality_id);

CREATE TABLE recruitment_request_cities
(
    recruitment_request_id UUID   NOT NULL,
    city_id                BIGINT NOT NULL,
    CONSTRAINT pk_recruitment_request_cities PRIMARY KEY (recruitment_request_id, city_id),
    CONSTRAINT fk_rr_cities_request FOREIGN KEY (recruitment_request_id) REFERENCES recruitment_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_cities_city FOREIGN KEY (city_id) REFERENCES cities (id) ON DELETE CASCADE
);

CREATE INDEX idx_rr_cities_city ON recruitment_request_cities (city_id);

CREATE TABLE recruitment_responses
(
    id                     UUID        NOT NULL,
    recruitment_request_id UUID        NOT NULL,
    student_id             UUID        NOT NULL,
    status                 VARCHAR(32) NOT NULL,
    created_at             TIMESTAMP,
    updated_at             TIMESTAMP,
    CONSTRAINT pk_recruitment_responses PRIMARY KEY (id),
    CONSTRAINT uc_recruitment_responses_request_student UNIQUE (recruitment_request_id, student_id),
    CONSTRAINT fk_rr_responses_request FOREIGN KEY (recruitment_request_id) REFERENCES recruitment_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_responses_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE
);

CREATE INDEX idx_recruitment_responses_student ON recruitment_responses (student_id);
