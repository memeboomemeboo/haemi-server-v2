-- #52: 활동 종류별 완료 플래그. 막대 그래프를 종류별 스택으로 그리기 위한 종류 구분.
-- 기존 참여 기록은 훈련(TrainingSessionCompleted)에서만 생성됐으므로 training_done=true로 백필한다.

ALTER TABLE elder_attendance_daily_participations
    ADD COLUMN training_done      BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN greeting_read_done BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN memory_viewed_done BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN replied_done       BOOLEAN NOT NULL DEFAULT false;

UPDATE elder_attendance_daily_participations SET training_done = true;

ALTER TABLE guardian_report_participations
    ADD COLUMN training_done      BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN greeting_read_done BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN memory_viewed_done BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN replied_done       BOOLEAN NOT NULL DEFAULT false;

UPDATE guardian_report_participations SET training_done = true;
