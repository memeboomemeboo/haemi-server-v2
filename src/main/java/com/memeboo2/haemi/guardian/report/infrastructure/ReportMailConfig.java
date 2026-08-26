package com.memeboo2.haemi.guardian.report.infrastructure;

import com.memeboo2.haemi.guardian.report.application.ReportMailPort;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;

/** 리포트 이메일 발송 어댑터. SMTP가 설정된 환경에서는 첨부 발송, 아니면 로깅 대체. */
@Configuration
public class ReportMailConfig {

    /** SMTP가 설정된 환경(운영·스테이징)에서 PDF를 첨부해 발송한다. */
    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    ReportMailPort smtpReportMailPort(JavaMailSender mailSender,
                                      @Value("${haemi.mail.from}") String from) {
        return new SmtpReportMailPort(mailSender, from);
    }

    /** SMTP 미설정(로컬·테스트)용 대체 구현. */
    @Bean
    @ConditionalOnMissingBean(ReportMailPort.class)
    ReportMailPort loggingReportMailPort() {
        Logger log = LoggerFactory.getLogger("ReportMail");
        return (toEmail, subject, bodyText, attachmentFilename, pdf) ->
                log.info("[리포트 메일 대체발송] to={}, subject={}, attachment={} ({} bytes)",
                        toEmail, subject, attachmentFilename, pdf.length);
    }

    static class SmtpReportMailPort implements ReportMailPort {

        private final JavaMailSender mailSender;
        private final String from;

        SmtpReportMailPort(JavaMailSender mailSender, String from) {
            this.mailSender = mailSender;
            this.from = from;
        }

        @Override
        public void sendReport(String toEmail, String subject, String bodyText,
                               String attachmentFilename, byte[] pdf) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
                helper.setFrom(from);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(bodyText, false);
                helper.addAttachment(attachmentFilename, new ByteArrayResource(pdf), "application/pdf");
                mailSender.send(message);
            } catch (MailException | jakarta.mail.MessagingException e) {
                throw new ReportMailDeliveryException(e);
            }
        }
    }

    static class ReportMailDeliveryException extends RuntimeException {
        ReportMailDeliveryException(Throwable cause) {
            super("리포트 메일 발송 실패", cause);
        }
    }
}
