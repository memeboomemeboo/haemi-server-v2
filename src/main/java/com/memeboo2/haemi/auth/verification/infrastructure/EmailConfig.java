package com.memeboo2.haemi.auth.verification.infrastructure;

import com.memeboo2.haemi.auth.verification.application.EmailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfig {

    /** 외부 이메일 어댑터가 없을 때만 개발용 대체 구현을 사용한다. */
    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    EmailSender loggingEmailSender() {
        return new LoggingEmailSender();
    }
}
