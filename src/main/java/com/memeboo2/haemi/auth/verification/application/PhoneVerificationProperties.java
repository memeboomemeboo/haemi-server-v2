package com.memeboo2.haemi.auth.verification.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "haemi.auth.verification")
public record PhoneVerificationProperties(
        @DefaultValue("5") int maxConfirmAttempts,
        @DefaultValue("5") int maxResendPerWindow,
        @DefaultValue("3600") long resendWindowSeconds
) {}
