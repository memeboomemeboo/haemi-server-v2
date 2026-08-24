package com.memeboo2.haemi.guardian.report.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** RPT-ATT-003 상태 기준: 주 5일↑ 🟢 / 3~4일 🟡 / 2일↓ 🟠. */
@ConfigurationProperties(prefix = "haemi.report")
public record ReportProperties(
        @DefaultValue("5") int goodThresholdDays,
        @DefaultValue("3") int normalThresholdDays,
        @DefaultValue("7") int weeklyWindowDays,
        @DefaultValue("4") int monthlyWindowWeeks
) {}
