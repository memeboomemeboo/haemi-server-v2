-- CIST-TRN-001: 인지 훈련 세션과 단계 진행 상태
CREATE TABLE elder_training_sessions (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    elder_id        UUID        NOT NULL,
    active_elder_id UUID,
    session_date    DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL,
    current_step    VARCHAR(20),
    current_question_number INTEGER,
    started_at      TIMESTAMPTZ NOT NULL,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uk_training_sessions_elder_date UNIQUE (elder_id, session_date),
    CONSTRAINT uk_training_sessions_active_elder UNIQUE (active_elder_id)
);

CREATE INDEX idx_training_sessions_elder_status
    ON elder_training_sessions (elder_id, status, started_at);

CREATE INDEX idx_training_sessions_completed_at
    ON elder_training_sessions (elder_id, status, completed_at);
