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
--    기존에 한 사람이 두 가족에 속한 데이터가 있으면 인덱스 생성이 실패하므로 먼저 결정적으로 정리한다:
--    가장 먼저 합류한 소속만 남기고, 나중 소속과 그 소속으로 생긴 어르신 링크를 함께 정리한다.
--    (링크를 남겨 두면 제약을 걸어도 다른 가족 어르신 데이터에 계속 접근할 수 있다.)
--    이 테이블들은 조회 시 deleted_at을 걸러내지 않으므로 soft delete로는 접근이 끊기지 않는다.
DELETE FROM guardian_elder_links l
  USING (
        SELECT id, user_id, family_id
          FROM (
                SELECT id, user_id, family_id,
                       row_number() OVER (PARTITION BY user_id ORDER BY created_at, id) AS rn
                  FROM guardian_family_members
                 WHERE deleted_at IS NULL
               ) ranked
         WHERE rn > 1
       ) surplus,
       guardian_elders e
 WHERE l.guardian_id = surplus.user_id
   AND l.elder_id = e.id
   AND e.family_id = surplus.family_id
   AND l.deleted_at IS NULL;

DELETE FROM guardian_family_members
 WHERE id IN (
        SELECT id
          FROM (
                SELECT id,
                       row_number() OVER (PARTITION BY user_id ORDER BY created_at, id) AS rn
                  FROM guardian_family_members
                 WHERE deleted_at IS NULL
               ) ranked
         WHERE rn > 1
       );

CREATE UNIQUE INDEX uk_family_member_user ON guardian_family_members(user_id) WHERE deleted_at IS NULL;
