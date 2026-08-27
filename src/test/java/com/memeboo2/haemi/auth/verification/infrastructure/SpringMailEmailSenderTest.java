package com.memeboo2.haemi.auth.verification.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/** SpringMailEmailSender의 메일 발송 위임 및 예외 변환 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class SpringMailEmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private SpringMailEmailSender sender;

    @BeforeEach
    void setUp() {
        MailProperties properties = new MailProperties("no-reply@haemi.local", "https://haemi.app");
        sender = new SpringMailEmailSender(mailSender, properties);
    }

    @Test
    void 인증번호_메일을_발신자_수신자_제목을_채워_발송한다() {
        sender.sendVerificationCode("user@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@haemi.local");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).isEqualTo("[해미] 인증번호 123456");
        assertThat(message.getText()).contains("123456");
    }

    @Test
    void 메일_발송이_실패하면_EmailDeliveryException으로_감싼다() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        assertThatThrownBy(() -> sender.sendVerificationCode("user@example.com", "654321"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasCauseInstanceOf(MailSendException.class);
    }
}
