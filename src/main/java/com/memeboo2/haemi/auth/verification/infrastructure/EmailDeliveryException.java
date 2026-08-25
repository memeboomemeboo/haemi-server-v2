package com.memeboo2.haemi.auth.verification.infrastructure;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;

/** SMTP 발송 실패. 인증번호를 받지 못한 사용자가 재시도할 수 있도록 5xx로 알린다. */
public class EmailDeliveryException extends DomainException {

    public EmailDeliveryException(Throwable cause) {
        super(ErrorCode.EMAIL_DELIVERY_FAILED, "인증번호 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        initCause(cause);
    }
}
