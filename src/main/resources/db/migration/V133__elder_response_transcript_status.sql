ALTER TABLE elder_responses ADD COLUMN transcript_status VARCHAR(20);

UPDATE elder_responses
SET transcript_status = CASE
    WHEN response_type = 'VOICE' AND transcript IS NOT NULL THEN 'COMPLETED'
    WHEN response_type = 'VOICE' THEN 'PENDING'
    ELSE 'NOT_APPLICABLE'
END;

ALTER TABLE elder_responses ALTER COLUMN transcript_status SET NOT NULL;
ALTER TABLE elder_responses ALTER COLUMN transcript_status SET DEFAULT 'NOT_APPLICABLE';
