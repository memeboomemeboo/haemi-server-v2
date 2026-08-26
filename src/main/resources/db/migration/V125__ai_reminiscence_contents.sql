-- #83 매일 08:00 개인화 회상 콘텐츠 배치 생성
-- 황정빈 대역: V100~V199

CREATE TABLE ai_reminiscence_contents (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    elder_id      UUID         NOT NULL,          -- FK 없음 (모듈 간 FK 금지)
    content_date  DATE         NOT NULL,
    content       VARCHAR(2000) NOT NULL,
    ai_generated  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    UUID,
    deleted_at    TIMESTAMPTZ
);

-- 어르신·날짜당 하나 (배치 재실행 시 upsert 대상)
CREATE UNIQUE INDEX uq_ai_reminiscence_elder_date
    ON ai_reminiscence_contents (elder_id, content_date);
