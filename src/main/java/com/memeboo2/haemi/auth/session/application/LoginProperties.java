package com.memeboo2.haemi.auth.session.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "haemi.auth.login")
public record LoginProperties(
        @DefaultValue("5") int maxFailedAttempts,
        @DefaultValue("3") int maxPinFailedAttempts,
        @DefaultValue("900") long lockDurationSeconds
) {}
