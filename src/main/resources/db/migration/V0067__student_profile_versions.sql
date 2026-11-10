CREATE TABLE student_profile_versions
(
    id             UUID PRIMARY KEY,
    student_id     UUID         NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    version_number INTEGER      NOT NULL,
    status         VARCHAR(32)  NOT NULL,
    snapshot       JSONB        NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    grace_ends_at  TIMESTAMP,
    CONSTRAINT uq_student_profile_version UNIQUE (student_id, version_number)
);

CREATE INDEX idx_student_profile_versions_student ON student_profile_versions (student_id, version_number DESC);
