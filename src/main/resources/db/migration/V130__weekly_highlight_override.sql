-- #100 M5: 보호자가 편집한 "이번 주 하이라이트" 문구 오버라이드
-- 황정빈 대역: V100~V199

CREATE TABLE weekly_highlight_overrides (
    id          UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    elder_id    UUID          NOT NULL,
    week_start  DATE          NOT NULL,
    content     VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by  UUID,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uk_weekly_highlight_elder_week UNIQUE (elder_id, week_start)
);

CREATE INDEX idx_weekly_highlight_elder ON weekly_highlight_overrides(elder_id);
