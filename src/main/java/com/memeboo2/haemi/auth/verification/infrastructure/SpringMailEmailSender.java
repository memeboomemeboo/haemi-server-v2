package com.memeboo2.haemi.auth.verification.infrastructure;

import com.memeboo2.haemi.auth.verification.application.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** SMTP 발송 어댑터. spring.mail.host가 설정된 환경에서만 등록된다. */
@RequiredArgsConstructor
public class SpringMailEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    @Override
    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(email);
        message.setSubject("[해미] 인증번호 " + code);
        message.setText("""
                해미 인증번호는 %s 입니다.
                5분 안에 입력해 주세요.
                """.formatted(code));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new EmailDeliveryException(e);
        }
    }
}
