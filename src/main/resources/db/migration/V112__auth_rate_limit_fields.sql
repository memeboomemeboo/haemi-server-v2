ALTER TABLE phone_verifications
    ADD COLUMN fail_count INT NOT NULL DEFAULT 0;

ALTER TABLE accounts
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMPTZ;
