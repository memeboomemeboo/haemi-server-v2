-- Phase 0-4: guardian/family + guardian/eldermanagement
-- 황정빈 대역: V100~V199

CREATE TABLE guardian_families (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    deleted_at  TIMESTAMPTZ
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
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID        NOT NULL,
    family_id   UUID        NOT NULL,
    name        VARCHAR(30) NOT NULL,
    birth_date  DATE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_elders_family_id ON guardian_elders(family_id);
CREATE INDEX idx_elders_user_id   ON guardian_elders(user_id);

CREATE TABLE guardian_elder_links (
    id           UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    guardian_id  UUID        NOT NULL,
    elder_id     UUID        NOT NULL,
    role         VARCHAR(20) NOT NULL DEFAULT '보호자',
    linked_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   UUID,
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT uq_guardian_elder UNIQUE (guardian_id, elder_id)
);

CREATE INDEX idx_elder_links_guardian_id ON guardian_elder_links(guardian_id);
CREATE INDEX idx_elder_links_elder_id    ON guardian_elder_links(elder_id);
