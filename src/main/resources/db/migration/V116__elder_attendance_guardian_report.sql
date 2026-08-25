CREATE TABLE elder_attendance_daily_participations (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    elder_id            UUID        NOT NULL,
    participation_date  DATE        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT uk_daily_participation_elder_date UNIQUE (elder_id, participation_date)
);

CREATE INDEX idx_daily_participation_elder ON elder_attendance_daily_participations(elder_id);

CREATE TABLE guardian_report_participations (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    elder_id            UUID        NOT NULL,
    participation_date  DATE        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT uk_report_participation_elder_date UNIQUE (elder_id, participation_date)
);

CREATE INDEX idx_report_participation_elder ON guardian_report_participations(elder_id);
