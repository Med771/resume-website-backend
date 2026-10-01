ALTER TABLE users
    ADD COLUMN recruiter_id UUID;

ALTER TABLE users
    ADD CONSTRAINT fk_users_recruiter
        FOREIGN KEY (recruiter_id) REFERENCES recruiters (id);

CREATE UNIQUE INDEX uc_users_recruiter_id ON users (recruiter_id)
    WHERE recruiter_id IS NOT NULL;
