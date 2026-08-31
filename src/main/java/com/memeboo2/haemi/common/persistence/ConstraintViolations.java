package com.memeboo2.haemi.common.persistence;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** DB 제약 위반이 어떤 제약에서 났는지 판별한다. */
public final class ConstraintViolations {

    // Postgres 메시지의 따옴표로 감싼 제약명 토큰만 뽑는다: ... unique constraint "uk_accounts_email"
    private static final Pattern QUOTED_TOKEN = Pattern.compile("\"([^\"]+)\"");

    private ConstraintViolations() {}

    /**
     * Hibernate가 제약명을 파싱해 주면 그것을 쓰고, 드라이버/버전에 따라 비어 있으면
     * 예외 메시지에서 이름을 찾는다.
     *
     * <p>제약명은 정확한 식별자이므로 부분 문자열이 아닌 정확 일치(대소문자 무시)로 비교한다.
     * 메시지 폴백도 원문 전체를 substring 매칭하지 않고 따옴표로 감싼 토큰만 추출해 비교한다.
     * 원문에는 사용자 입력(이메일·아이디 값)이 섞여 들어와, 그 값이 우연히 다른 제약명을
     * 포함하면 엉뚱한 제약으로 오분류될 수 있기 때문이다.</p>
     */
    public static boolean isViolationOf(DataIntegrityViolationException exception, String constraintName) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                if (name != null && !name.isBlank()) {
                    return name.equalsIgnoreCase(constraintName);
                }
            }
            cause = cause.getCause();
        }
        String message = exception.getMostSpecificCause().getMessage();
        return message != null && matchesQuotedToken(message, constraintName);
    }

    private static boolean matchesQuotedToken(String message, String constraintName) {
        Matcher matcher = QUOTED_TOKEN.matcher(message);
        while (matcher.find()) {
            if (matcher.group(1).equalsIgnoreCase(constraintName)) {
                return true;
            }
        }
        return false;
    }
}
