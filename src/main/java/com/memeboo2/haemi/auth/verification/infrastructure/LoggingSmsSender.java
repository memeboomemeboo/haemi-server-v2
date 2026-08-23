package com.memeboo2.haemi.auth.verification.infrastructure;

import com.memeboo2.haemi.auth.verification.application.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 개발 환경용 대체 구현. 운영 환경에서는 SMS 공급자 어댑터 빈으로 교체한다. */
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void sendVerificationCode(String phone, String code) {
        log.info("[DEV ONLY] SMS verification code generated for phone ending {}", phone.substring(Math.max(0, phone.length() - 4)));
    }
}
