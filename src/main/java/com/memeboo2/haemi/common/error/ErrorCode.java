package com.memeboo2.haemi.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 공통
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ROLE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    CARE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 어르신에 대한 접근 권한이 없습니다."),
    NOT_RESOURCE_OWNER(HttpStatus.FORBIDDEN, "본인이 등록한 리소스만 수정할 수 있습니다."),
    FAMILY_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "가족 등록 상한을 초과했습니다."),
    LAST_GUARDIAN_CANNOT_LEAVE(HttpStatus.CONFLICT, "마지막 보호자는 연결을 해제할 수 없습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    // 인증
    LOGIN_ID_ALREADY_TAKEN(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "이메일 인증이 필요합니다."),
    EMAIL_ALREADY_TAKEN(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    AUTH_VERIFICATION_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "인증 시도 횟수를 초과했습니다. 인증을 다시 요청해 주세요."),
    AUTH_VERIFICATION_RESEND_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "인증번호 요청이 너무 잦습니다. 잠시 후 다시 시도해 주세요."),
    AUTH_ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 시도 횟수를 초과해 계정이 일시적으로 잠겼습니다."),

    // 하루 한마디
    DAILY_CARE_ALREADY_SENT(HttpStatus.CONFLICT, "오늘은 이미 하루 한마디를 전했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
