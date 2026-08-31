package com.memeboo2.haemi.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 공통
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ROLE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    CARE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 어르신에 대한 접근 권한이 없습니다."),
    NOT_RESOURCE_OWNER(HttpStatus.FORBIDDEN, "본인이 등록한 리소스만 수정할 수 있습니다."),
    FAMILY_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "가족 등록 상한을 초과했습니다."),
    FAMILY_INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "초대 코드 생성에 실패했습니다. 잠시 후 다시 시도해 주세요."),
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
    EMAIL_DELIVERY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "인증번호 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    AUTH_ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 시도 횟수를 초과해 계정이 일시적으로 잠겼습니다."),
    AUTH_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다. 다시 로그인해 주세요."),

    // 하루 한마디
    DAILY_CARE_ALREADY_SENT(HttpStatus.CONFLICT, "오늘은 이미 하루 한마디를 전했습니다."),

    // 인지 훈련
    TRAINING_SESSION_ALREADY_STARTED(HttpStatus.CONFLICT, "이미 해당 날짜의 인지 훈련 세션이 있습니다."),
    TRAINING_MATERIAL_UNAVAILABLE(HttpStatus.CONFLICT, "인지 훈련에 필요한 사진 자료를 준비하고 있습니다."),

    // 인지 리포트
    REPORT_PDF_RENDER_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "리포트 PDF 생성에 실패했습니다."),

    // 미디어
    MEDIA_DUPLICATE_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "같은 파일이 이미 확정되었습니다. 업로드 요청을 다시 시도해 주세요."),
    MEDIA_CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 변환에 실패했습니다.");

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
