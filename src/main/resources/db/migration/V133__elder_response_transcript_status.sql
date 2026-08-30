ALTER TABLE elder_responses ADD COLUMN transcript_status VARCHAR(20);

UPDATE elder_responses
SET transcript_status = CASE
    WHEN response_type = 'VOICE' AND transcript IS NOT NULL THEN 'COMPLETED'
    -- 기존 미전사 음성에는 VoiceResponseCreated 이벤트가 없으므로 PENDING으로 두면 영구 대기 상태가 된다.
    WHEN response_type = 'VOICE' THEN 'FAILED'
    ELSE 'NOT_APPLICABLE'
END;

ALTER TABLE elder_responses ALTER COLUMN transcript_status SET NOT NULL;
ALTER TABLE elder_responses ALTER COLUMN transcript_status SET DEFAULT 'NOT_APPLICABLE';
