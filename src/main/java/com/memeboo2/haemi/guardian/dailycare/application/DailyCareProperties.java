package com.memeboo2.haemi.guardian.dailycare.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "haemi.dailycare")
public record DailyCareProperties(
        @DefaultValue("30") int retentionDays,
        @DefaultValue("30") int inboxLookBackDays,
        @DefaultValue("60") int maxVoiceDurationSeconds
) {}
