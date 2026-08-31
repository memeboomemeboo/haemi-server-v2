package com.memeboo2.haemi.common.persistence;

import com.memeboo2.haemi.common.security.JwtPrincipal;
import com.memeboo2.haemi.common.time.HaemiClock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
public class AuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(authentication -> authentication.getPrincipal())
                .filter(JwtPrincipal.class::isInstance)
                .map(JwtPrincipal.class::cast)
                .map(JwtPrincipal::userId);
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider(HaemiClock clock) {
        return () -> Optional.of(clock.now());
    }
}
