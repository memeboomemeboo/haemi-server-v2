package com.memeboo2.haemi.auth.verification.application;

/** 실제 이메일 공급자 어댑터가 구현하는 발송 포트. */
public interface EmailSender {

    void sendVerificationCode(String email, String code);
}
