package com.memeboo2.haemi.auth.verification.infrastructure;

import com.memeboo2.haemi.auth.verification.application.SmsSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmsConfig {

    /** 외부 SMS 어댑터가 없을 때만 개발용 대체 구현을 사용한다. */
    @Bean
    @ConditionalOnMissingBean(SmsSender.class)
    SmsSender loggingSmsSender() {
        return new LoggingSmsSender();
    }
}
