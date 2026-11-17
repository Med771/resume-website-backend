ALTER TABLE users DROP CONSTRAINT chk_users_account_status;

ALTER TABLE users ADD CONSTRAINT chk_users_account_status
    CHECK (account_status IN (
        'PENDING_APPROVAL',
        'APPROVED',
        'REJECTED',
        'REGISTRATION',
        'IN_REVISION'
    ));
