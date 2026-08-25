-- #57 리뷰(P2): refresh 토큰 문자열을 유일하게 강제한다.
-- createRefreshToken에 jti(랜덤 claim)가 들어가 토큰이 전역 유일해졌으므로,
-- token만으로 단건 조회하는 경로에서 다중 결과가 나올 여지를 DB 제약으로 차단한다.

-- 리뷰(P1): jti 도입 이전에는 서로 다른 기기에 동일 토큰 문자열이 저장될 수 있었다.
-- 기존 DB에 그런 중복 행이 하나라도 있으면 유니크 인덱스 생성이 실패해 배포가 중단되므로,
-- 인덱스 생성 전에 토큰별로 1개(정렬상 가장 앞선 id)만 남기고 나머지를 정리한다.
-- MIN(uuid)는 PostgreSQL에 없어 이식성을 위해 VARCHAR로 캐스팅한다(H2·PG 공통).
DELETE FROM refresh_tokens
WHERE CAST(id AS VARCHAR) NOT IN (
    SELECT MIN(CAST(id AS VARCHAR)) FROM refresh_tokens GROUP BY token
);

DROP INDEX IF EXISTS idx_refresh_tokens_token;

CREATE UNIQUE INDEX uk_refresh_tokens_token ON refresh_tokens (token);
