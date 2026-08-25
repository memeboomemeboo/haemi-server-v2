-- CIST-TRN-002~005: 고정 문항, 응답, 난이도, 큐레이션 노출
-- 일별 출석 원천은 V116의 elder_attendance_daily_participations를 사용한다.
CREATE TABLE platform_content_items (
    id                  UUID         NOT NULL PRIMARY KEY,
    title               VARCHAR(100) NOT NULL,
    image_key           VARCHAR(500) NOT NULL,
    content_year        INTEGER,
    answer_keywords     VARCHAR(500) NOT NULL,
    region              VARCHAR(20)  NOT NULL,
    recommended_min_age INTEGER,
    recommended_max_age INTEGER,
    available_until     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          UUID,
    deleted_at          TIMESTAMPTZ
);

CREATE TABLE platform_content_exposures (
    id          UUID        NOT NULL PRIMARY KEY,
    elder_id    UUID        NOT NULL,
    content_id  UUID        NOT NULL,
    exposed_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  UUID,
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_content_exposures_elder_exposed_at
    ON platform_content_exposures (elder_id, exposed_at);

CREATE TABLE elder_training_questions (
    id              UUID         NOT NULL PRIMARY KEY,
    session_id      UUID         NOT NULL,
    question_number INTEGER      NOT NULL,
    question_type   VARCHAR(20)  NOT NULL,
    question_kind   VARCHAR(30)  NOT NULL,
    answer_mode     VARCHAR(20)  NOT NULL,
    prompt          VARCHAR(300) NOT NULL,
    image_key       VARCHAR(500),
    material_id     UUID,
    material_source VARCHAR(20),
    material_title  VARCHAR(100),
    answer_key      VARCHAR(200) NOT NULL,
    year_tolerance  INTEGER      NOT NULL,
    hint            VARCHAR(200),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uk_training_questions_session_number UNIQUE (session_id, question_number)
);

CREATE TABLE elder_training_question_options (
    question_id   UUID         NOT NULL,
    option_order  INTEGER      NOT NULL,
    option_text   VARCHAR(100) NOT NULL,
    PRIMARY KEY (question_id, option_order)
);

CREATE TABLE elder_training_answers (
    id              UUID        NOT NULL PRIMARY KEY,
    session_id      UUID        NOT NULL,
    question_id     UUID        NOT NULL,
    elder_id        UUID        NOT NULL,
    question_number INTEGER     NOT NULL,
    question_type   VARCHAR(20) NOT NULL,
    selected_option VARCHAR(100),
    text_answer     VARCHAR(500),
    voice_media_key VARCHAR(500),
    correct         BOOLEAN,
    answered_at     TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uk_training_answers_session_number UNIQUE (session_id, question_number)
);

CREATE INDEX idx_training_answers_session_type
    ON elder_training_answers (session_id, question_type);

CREATE TABLE elder_training_difficulties (
    id                    UUID        NOT NULL PRIMARY KEY,
    elder_id              UUID        NOT NULL,
    question_type         VARCHAR(20) NOT NULL,
    level                 VARCHAR(20) NOT NULL,
    consecutive_high_days INTEGER     NOT NULL,
    last_evaluated_date   DATE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID,
    deleted_at            TIMESTAMPTZ,
    CONSTRAINT uk_training_difficulty_elder_type UNIQUE (elder_id, question_type)
);
