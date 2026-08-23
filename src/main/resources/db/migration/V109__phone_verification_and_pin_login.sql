ALTER TABLE accounts
    ADD COLUMN pin_login_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE phone_verifications (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    phone       VARCHAR(20) NOT NULL,
    code_hash   VARCHAR(100) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_phone_verifications_phone ON phone_verifications(phone);
