ALTER TABLE users
    ADD COLUMN IF NOT EXISTS account_status VARCHAR(32) NOT NULL DEFAULT 'APPROVED';

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS hints_disabled BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users SET account_status = 'APPROVED' WHERE account_status IS NULL OR TRIM(account_status) = '';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_users_role'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT chk_users_role
            CHECK (role IN ('STUDENT', 'RECRUITER', 'ADMIN'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_users_account_status'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT chk_users_account_status
            CHECK (account_status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED'));
    END IF;
END $$;
