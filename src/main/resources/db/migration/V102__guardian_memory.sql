-- Phase guardian/memory: 추억 앨범
-- 황정빈 대역: V100~V199

CREATE TABLE guardian_memories (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    elder_id    UUID        NOT NULL,           -- FK 없음 (모듈 간 FK 금지)
    title       VARCHAR(100) NOT NULL,
    memo        VARCHAR(300),
    message     VARCHAR(100) NOT NULL,
    memory_year INT,
    responded   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    deleted_at  TIMESTAMPTZ
);

CREATE TABLE guardian_memory_images (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    memory_id   UUID        NOT NULL REFERENCES guardian_memories(id),
    storage_key VARCHAR(500) NOT NULL,
    position    INT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_guardian_memories_elder_id   ON guardian_memories (elder_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_guardian_memories_created_by ON guardian_memories (created_by) WHERE deleted_at IS NULL;
CREATE INDEX idx_guardian_memory_images_memory_id ON guardian_memory_images (memory_id);
