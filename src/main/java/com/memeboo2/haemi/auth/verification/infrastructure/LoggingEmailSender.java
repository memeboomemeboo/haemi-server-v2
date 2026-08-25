package com.memeboo2.haemi.auth.verification.infrastructure;

import com.memeboo2.haemi.auth.verification.application.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 개발 환경용 대체 구현. 운영 환경에서는 이메일 공급자 어댑터 빈으로 교체한다. */
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void sendVerificationCode(String email, String code) {
        log.info("[DEV ONLY] email verification code generated for {}", mask(email));
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
