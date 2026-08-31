package com.memeboo2.haemi.common.error;

import com.memeboo2.haemi.common.web.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomain(DomainException ex) {
        ErrorCode code = ex.getErrorCode();
        // 5xx로 매핑되는 도메인 예외는 서버 결함이므로 스택트레이스와 함께 error 레벨로 남긴다.
        // 4xx는 정상적인 클라이언트 오류이므로 로그 노이즈를 줄이기 위해 debug 레벨로만 남긴다.
        if (code.getStatus().is5xxServerError()) {
            log.error("도메인 예외(5xx) code={} status={}", code.name(), code.getStatus().value(), ex);
        } else {
            log.debug("도메인 예외(4xx) code={} message={}", code.name(), ex.getMessage());
        }
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.error(code.name(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        // 필드 오류 우선, 없으면 클래스 레벨 ObjectError로 폴백
        String field = null;
        String message = null;
        FieldError fieldError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        if (fieldError != null) {
            field = fieldError.getField();
            message = fieldError.getDefaultMessage();
        } else {
            ObjectError objectError = ex.getBindingResult().getAllErrors().stream().findFirst().orElse(null);
            if (objectError != null) {
                message = objectError.getDefaultMessage();
            }
        }
        if (message == null) {
            message = ErrorCode.INVALID_INPUT.getDefaultMessage();
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.name(), message, field));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        String message = ex.getAllErrors().stream()
                .findFirst()
                .map(e -> e.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getDefaultMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.name(), message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse(ErrorCode.INVALID_INPUT.getDefaultMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.name(), message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestValueException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.name(), ErrorCode.INVALID_INPUT.getDefaultMessage()));
    }

    // Spring MVC/Boot가 던지는 4xx/5xx 프레임워크 예외 (NoResourceFoundException, HttpRequestMethodNotSupportedException 등)를
    // 본래 상태 코드로 그대로 통과시킨다. catch-all로 500이 되는 것을 방지하고 로그 노이즈를 억제한다.
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleErrorResponse(ErrorResponseException ex) {
        if (ex.getStatusCode().is4xxClientError()) {
            log.debug("프레임워크 4xx status={} message={}", ex.getStatusCode().value(), ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode())
                    .body(ApiResponse.error(ErrorCode.INVALID_INPUT.name(), ex.getMessage()));
        }
        log.error("프레임워크 5xx status={}", ex.getStatusCode().value(), ex);
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        // 처리되지 않은 예외는 원인 추적이 유일하게 가능한 지점이므로 반드시 스택트레이스를 남긴다.
        // MDC의 requestId(RequestIdFilter)가 로그 패턴에 포함되어 응답 헤더 X-Request-Id와 대조된다.
        log.error("처리되지 않은 예외 발생", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }
}
