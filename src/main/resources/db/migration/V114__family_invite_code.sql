ALTER TABLE guardian_families
    ADD COLUMN invite_code VARCHAR(12);

UPDATE guardian_families
    SET invite_code = upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8))
    WHERE invite_code IS NULL;

ALTER TABLE guardian_families
    ALTER COLUMN invite_code SET NOT NULL,
    ADD CONSTRAINT uk_guardian_families_invite_code UNIQUE (invite_code);
