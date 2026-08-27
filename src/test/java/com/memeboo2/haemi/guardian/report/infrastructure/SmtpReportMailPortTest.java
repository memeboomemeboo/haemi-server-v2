package com.memeboo2.haemi.guardian.report.infrastructure;

import com.memeboo2.haemi.guardian.report.infrastructure.ReportMailConfig.ReportMailDeliveryException;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportMailConfig.SmtpReportMailPort;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpReportMailPortTest {

    @Mock JavaMailSender mailSender;
    @Mock MimeMessage mimeMessage;

    private SmtpReportMailPort port;

    @BeforeEach
    void setUp() {
        port = new SmtpReportMailPort(mailSender, "noreply@haemi.example");
    }

    @Test
    void 정상_발송이면_mailSender로_전송한다() {
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);

        port.sendReport("guardian@example.com", "제목", "본문", "report.pdf", new byte[]{1, 2, 3});

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void 발송_중_MailException이_발생하면_ReportMailDeliveryException으로_감싼다() {
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
        org.mockito.Mockito.doThrow(new MailSendException("smtp down"))
                .when(mailSender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));

        assertThatThrownBy(() ->
                port.sendReport("guardian@example.com", "제목", "본문", "report.pdf", new byte[]{1, 2, 3}))
                .isInstanceOf(ReportMailDeliveryException.class)
                .hasCauseInstanceOf(MailSendException.class);
    }
}
