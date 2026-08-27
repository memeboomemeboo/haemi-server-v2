package com.memeboo2.haemi.auth.verification.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

class LoggingEmailSenderTest {

    private final LoggingEmailSender sender = new LoggingEmailSender();

    @Test
    void 인증코드_전송이_예외없이_로그로_출력된다() {
        assertThatNoException().isThrownBy(() ->
                sender.sendVerificationCode("user@example.com", "123456"));
    }

    @Test
    void 짧은_이메일도_마스킹_처리된다() {
        assertThatNoException().isThrownBy(() ->
                sender.sendVerificationCode("a@b.com", "000000"));
    }

    @Test
    void 한글자_로컬파트도_마스킹된다() {
        assertThatNoException().isThrownBy(() ->
                sender.sendVerificationCode("x@domain.com", "111111"));
    }
}
