package com.memeboo2.haemi.auth.verification.infrastructure;

import com.memeboo2.haemi.auth.verification.application.EmailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class EmailConfig {

    /** SMTP가 설정된 환경(운영·스테이징)에서는 실제로 메일을 보낸다. */
    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    EmailSender springMailEmailSender(JavaMailSender mailSender, MailProperties properties) {
        return new SpringMailEmailSender(mailSender, properties);
    }

    /** SMTP 설정이 없는 로컬·테스트 환경용 대체 구현. */
    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    EmailSender loggingEmailSender() {
        return new LoggingEmailSender();
    }
}
