-- SMS(휴대폰) 인증 제거 → 이메일 인증 전환 + 동시성 결함 방어 제약

-- 1) 계정 이메일 (보호자 가입 시 인증된 이메일)
ALTER TABLE accounts
    ADD COLUMN email VARCHAR(255);

CREATE UNIQUE INDEX uk_accounts_email ON accounts(email) WHERE email IS NOT NULL AND deleted_at IS NULL;

-- 2) 이메일 인증 테이블 (phone_verifications 대체)
CREATE TABLE email_verifications (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    code_hash   VARCHAR(100) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    verified_at TIMESTAMPTZ,
    consumed_at TIMESTAMPTZ,
    fail_count  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  UUID,
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_email_verifications_email ON email_verifications(email);

DROP TABLE phone_verifications;

-- 3) 발송 제한 카운터 (고정 윈도우). 프로세스 로컬 락 대신 DB 원자적 upsert로 다중 인스턴스에서도 유효하다.
CREATE TABLE auth_verification_rate_limits (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    rate_key      VARCHAR(255) NOT NULL,
    window_start  TIMESTAMPTZ  NOT NULL,
    attempt_count INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    UUID,
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT uk_verification_rate_limit UNIQUE (rate_key, window_start)
);

-- 4) 보호자 1인 1가족(R2)을 DB 제약으로 강제한다. 애플리케이션 선검사만으로는 동시 요청을 막지 못한다.
--    기존 데이터에 같은 user_id가 두 가족에 남아 있으면 인덱스 생성이 실패해 앱 기동 자체가 막힌다.
--    이미 깨진 데이터 때문에 배포가 멈추지는 않도록, 중복이 없을 때만 만들고 있으면 경고만 남긴다.
--    경고가 보이면 아래 쿼리로 중복을 정리한 뒤 인덱스를 수동 생성한다.
--    SELECT user_id FROM guardian_family_members WHERE deleted_at IS NULL
--     GROUP BY user_id HAVING count(*) > 1;
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM guardian_family_members WHERE deleted_at IS NULL
         GROUP BY user_id HAVING count(*) > 1
    ) THEN
        RAISE WARNING '보호자 1인 1가족 제약(uk_family_member_user)을 건너뜁니다 — 중복 소속 데이터가 있습니다.';
    ELSE
        CREATE UNIQUE INDEX uk_family_member_user ON guardian_family_members(user_id) WHERE deleted_at IS NULL;
    END IF;
END $$;
