-- RPT-ATT-004: CIST 완료 이벤트에서 적재하는 guardian/report 전용 인지 결과 스냅샷
CREATE TABLE guardian_report_cognitive_results (
    id                   UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    elder_id             UUID        NOT NULL,
    session_id           UUID        NOT NULL,
    session_date         DATE        NOT NULL,
    cognitive_area       VARCHAR(20) NOT NULL,
    scored_answer_count  INTEGER     NOT NULL,
    correct_answer_count INTEGER     NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT uk_report_cognitive_result_session_area UNIQUE (session_id, cognitive_area),
    CONSTRAINT ck_report_cognitive_result_counts
        CHECK (scored_answer_count >= 0 AND correct_answer_count >= 0 AND correct_answer_count <= scored_answer_count)
);

CREATE INDEX idx_report_cognitive_result_elder_date
    ON guardian_report_cognitive_results (elder_id, session_date);
