-- auth: accounts, refresh_tokens

CREATE TABLE accounts (
    id          UUID        NOT NULL PRIMARY KEY,
    role        VARCHAR(20) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    login_id    VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    pin_hash    VARCHAR(100),
    birth_date  VARCHAR(10),
    phone       VARCHAR(20),
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    created_by  UUID,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uk_accounts_login_id UNIQUE (login_id)
);

CREATE TABLE refresh_tokens (
    id          UUID        NOT NULL PRIMARY KEY,
    account_id  UUID        NOT NULL,
    token       VARCHAR(512) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_tokens_account_id ON refresh_tokens (account_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
