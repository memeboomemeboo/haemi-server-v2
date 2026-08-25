package com.memeboo2.haemi.auth.verification.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "haemi.mail")
public record MailProperties(@DefaultValue("no-reply@haemi.local") String from,
        @DefaultValue("") String publicUrl) {}
