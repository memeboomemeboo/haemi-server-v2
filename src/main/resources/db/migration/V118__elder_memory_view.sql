-- #55: 어르신 추억 열람 기록. (elder_id, memory_id) 당 최초 열람 1행.
CREATE TABLE elder_memory_views (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    elder_id         UUID        NOT NULL,
    memory_id        UUID        NOT NULL,
    first_viewed_at  TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_memory_view_elder_memory UNIQUE (elder_id, memory_id)
);

CREATE INDEX idx_memory_view_elder ON elder_memory_views(elder_id);
