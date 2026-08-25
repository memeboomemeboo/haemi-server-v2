-- #57 리뷰(P2): refresh 토큰 문자열을 유일하게 강제한다.
-- createRefreshToken에 jti(랜덤 claim)가 들어가 토큰이 전역 유일해졌으므로,
-- token만으로 단건 조회하는 경로에서 다중 결과가 나올 여지를 DB 제약으로 차단한다.
DROP INDEX IF EXISTS idx_refresh_tokens_token;

CREATE UNIQUE INDEX uk_refresh_tokens_token ON refresh_tokens (token);
