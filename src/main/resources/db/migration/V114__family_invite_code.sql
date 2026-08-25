ALTER TABLE guardian_families
    ADD COLUMN invite_code VARCHAR(12);

-- row_number 기반 결정적 배정 — 무작위 생성은 8자리 충돌 시 마이그레이션 전체가
-- 실패해 애플리케이션이 기동하지 못한다. 기존 가족에는 절대 충돌하지 않는
-- 순번 코드를 부여하고, 신규 가족은 애플리케이션의 SecureInviteCodeGenerator를 사용한다.
WITH numbered AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM guardian_families
    WHERE invite_code IS NULL
)
UPDATE guardian_families gf
SET invite_code = 'MIG' || lpad(numbered.rn::text, 9, '0')
FROM numbered
WHERE gf.id = numbered.id;

ALTER TABLE guardian_families
    ALTER COLUMN invite_code SET NOT NULL,
    ADD CONSTRAINT uk_guardian_families_invite_code UNIQUE (invite_code);
