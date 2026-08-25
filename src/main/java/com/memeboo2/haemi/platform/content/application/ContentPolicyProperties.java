package com.memeboo2.haemi.platform.content.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "haemi.content")
public record ContentPolicyProperties(
        @DefaultValue("7") int cooldownDays,
        @DefaultValue("20") int depletionThreshold
) {}
