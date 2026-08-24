-- Phase elder/response: 추억 앨범 답변
-- 황정빈 대역: V100~V199

CREATE TABLE elder_responses (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    memory_id       UUID        NOT NULL,       -- FK 없음 (모듈 간 FK 금지)
    elder_id        UUID        NOT NULL,       -- FK 없음
    response_type   VARCHAR(20) NOT NULL,
    text            VARCHAR(100),
    media_key       VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    deleted_at      TIMESTAMPTZ
);

CREATE TABLE elder_response_emotions (
    response_id UUID        NOT NULL REFERENCES elder_responses(id),
    emotion     VARCHAR(20) NOT NULL
);

CREATE INDEX idx_elder_responses_memory_id ON elder_responses (memory_id);
CREATE INDEX idx_elder_responses_elder_id  ON elder_responses (elder_id);
