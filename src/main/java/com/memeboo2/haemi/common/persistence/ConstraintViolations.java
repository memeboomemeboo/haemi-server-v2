package com.memeboo2.haemi.common.persistence;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;

/** DB 제약 위반이 어떤 제약에서 났는지 판별한다. */
public final class ConstraintViolations {

    private ConstraintViolations() {}

    /**
     * Hibernate가 제약명을 파싱해 주면 그것을 쓰고, 드라이버/버전에 따라 비어 있으면
     * 예외 메시지에서 이름을 찾는다. 메시지 문자열만 믿으면 표현이 조금만 달라져도
     * 제약을 오분류해 엉뚱한 응답이 나간다.
     */
    public static boolean isViolationOf(DataIntegrityViolationException exception, String constraintName) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                if (name != null && !name.isBlank()) {
                    return name.toLowerCase(Locale.ROOT).contains(constraintName.toLowerCase(Locale.ROOT));
                }
            }
            cause = cause.getCause();
        }
        String message = exception.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(constraintName.toLowerCase(Locale.ROOT));
    }
}
