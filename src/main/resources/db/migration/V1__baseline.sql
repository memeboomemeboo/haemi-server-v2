-- ============================================================
-- V1__baseline.sql
-- 신규 환경용 전체 스키마 baseline (V100~V105 통합)
-- 기존 환경(V100+ 적용됨)은 flyway.baseline-on-migrate=true로 skip
-- ============================================================

-- ── auth ──────────────────────────────────────────────────

CREATE TABLE accounts (
    id            UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    role          VARCHAR(20)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    login_id      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    pin_hash      VARCHAR(100),
    birth_date    VARCHAR(10),
    phone         VARCHAR(20),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    UUID,
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT uk_accounts_login_id UNIQUE (login_id)
);

CREATE TABLE refresh_tokens (
    id         UUID        NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID        NOT NULL,
    token      VARCHAR(512) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_account_id ON refresh_tokens (account_id);
CREATE INDEX idx_refresh_tokens_token      ON refresh_tokens (token);

-- ── guardian/family + eldermanagement ─────────────────────

CREATE TABLE guardian_families (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE guardian_family_members (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    family_id   UUID        NOT NULL REFERENCES guardian_families(id),
    user_id     UUID        NOT NULL,
    member_type VARCHAR(20) NOT NULL DEFAULT 'GUARDIAN',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_family_member UNIQUE (family_id, user_id)
);

CREATE INDEX idx_family_members_user_id ON guardian_family_members(user_id);

CREATE TABLE guardian_elders (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID        NOT NULL,
    family_id  UUID        NOT NULL,
    name       VARCHAR(30) NOT NULL,
    birth_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_elders_family_id ON guardian_elders(family_id);
CREATE INDEX idx_elders_user_id   ON guardian_elders(user_id);

CREATE TABLE guardian_elder_links (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    guardian_id UUID        NOT NULL,
    elder_id    UUID        NOT NULL,
    role        VARCHAR(20) NOT NULL DEFAULT '보호자',
    linked_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_guardian_elder UNIQUE (guardian_id, elder_id)
);

CREATE INDEX idx_elder_links_guardian_id ON guardian_elder_links(guardian_id);
CREATE INDEX idx_elder_links_elder_id    ON guardian_elder_links(elder_id);

-- ── platform/media ─────────────────────────────────────────

CREATE TABLE media_uploads (
    id                       UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    media_type               VARCHAR(30)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    storage_key              VARCHAR(500) NOT NULL,
    original_filename        VARCHAR(255) NOT NULL,
    content_type             VARCHAR(100) NOT NULL,
    declared_size_bytes      BIGINT       NOT NULL,
    uploader_id              UUID         NOT NULL,
    presigned_url_expires_at TIMESTAMPTZ  NOT NULL,
    retain_until             TIMESTAMPTZ,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by               UUID,
    deleted_at               TIMESTAMPTZ
);

CREATE INDEX idx_media_uploads_uploader_id  ON media_uploads (uploader_id);
CREATE INDEX idx_media_uploads_status       ON media_uploads (status);
CREATE INDEX idx_media_uploads_retain_until ON media_uploads (retain_until) WHERE retain_until IS NOT NULL;

-- ── guardian/memory ─────────────────────────────────────────

CREATE TABLE guardian_memories (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    elder_id    UUID         NOT NULL,
    title       VARCHAR(100) NOT NULL,
    memo        VARCHAR(300),
    message     VARCHAR(100) NOT NULL,
    memory_year INT,
    responded   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  UUID,
    deleted_at  TIMESTAMPTZ
);

CREATE TABLE guardian_memory_images (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    memory_id   UUID         NOT NULL REFERENCES guardian_memories(id),
    storage_key VARCHAR(500) NOT NULL,
    position    INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_guardian_memories_elder_id        ON guardian_memories (elder_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_guardian_memories_created_by      ON guardian_memories (created_by) WHERE deleted_at IS NULL;
CREATE INDEX idx_guardian_memory_images_memory_id  ON guardian_memory_images (memory_id);

-- ── elder/response ──────────────────────────────────────────

CREATE TABLE elder_responses (
    id            UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    memory_id     UUID        NOT NULL,
    elder_id      UUID        NOT NULL,
    response_type VARCHAR(20) NOT NULL,
    text          VARCHAR(100),
    media_key     VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by    UUID,
    deleted_at    TIMESTAMPTZ
);

CREATE TABLE elder_response_emotions (
    response_id UUID        NOT NULL REFERENCES elder_responses(id),
    emotion     VARCHAR(20) NOT NULL
);

CREATE INDEX idx_elder_responses_memory_id ON elder_responses (memory_id);
CREATE INDEX idx_elder_responses_elder_id  ON elder_responses (elder_id);

-- ── guardian/dailycare ──────────────────────────────────────

CREATE TABLE guardian_daily_cares (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    guardian_id      UUID        NOT NULL,
    elder_id         UUID        NOT NULL,
    care_date        DATE        NOT NULL,
    care_type        VARCHAR(10) NOT NULL,
    text             VARCHAR(100),
    media_key        VARCHAR(500),
    duration_seconds INTEGER,
    retain_until     TIMESTAMPTZ NOT NULL,
    viewed_at        TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       UUID,
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT pk_daily_cares PRIMARY KEY (id),
    CONSTRAINT uk_daily_care_guardian_elder_date UNIQUE (guardian_id, elder_id, care_date)
);

CREATE INDEX idx_daily_cares_elder_date   ON guardian_daily_cares (elder_id, care_date);
CREATE INDEX idx_daily_cares_retain_until ON guardian_daily_cares (retain_until);
