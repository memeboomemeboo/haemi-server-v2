package com.memeboo2.haemi.auth.verification.application;

/** 실제 SMS 공급자 어댑터가 구현하는 발송 포트. */
public interface SmsSender {

    void sendVerificationCode(String phone, String code);
}
