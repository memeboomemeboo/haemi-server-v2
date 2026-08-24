ALTER TABLE refresh_tokens
    ADD COLUMN device_id VARCHAR(100) NOT NULL DEFAULT 'legacy';

CREATE UNIQUE INDEX uk_refresh_tokens_account_device
    ON refresh_tokens (account_id, device_id);
