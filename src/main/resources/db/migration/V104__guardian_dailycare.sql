-- guardian_daily_cares: 하루 한마디 (보호자 → 어르신)
CREATE TABLE guardian_daily_cares (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    guardian_id     UUID        NOT NULL,
    elder_id        UUID        NOT NULL,
    care_date       DATE        NOT NULL,
    care_type       VARCHAR(10) NOT NULL,
    text            VARCHAR(100),
    media_key       VARCHAR(500),
    duration_seconds INTEGER,
    retain_until    TIMESTAMPTZ NOT NULL,
    viewed_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    created_by      UUID,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT pk_daily_cares PRIMARY KEY (id),
    CONSTRAINT uk_daily_care_guardian_elder_date UNIQUE (guardian_id, elder_id, care_date)
);

CREATE INDEX idx_daily_cares_elder_date   ON guardian_daily_cares (elder_id, care_date);
CREATE INDEX idx_daily_cares_retain_until ON guardian_daily_cares (retain_until);
