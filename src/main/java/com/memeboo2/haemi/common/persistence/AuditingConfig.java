package com.memeboo2.haemi.common.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        // SecurityContext에서 현재 사용자 ID를 꺼냄 (auth 모듈 완성 후 연결)
        return Optional::empty;
    }
}
