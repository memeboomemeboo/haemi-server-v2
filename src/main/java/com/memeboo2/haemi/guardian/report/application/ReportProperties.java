package com.memeboo2.haemi.guardian.report.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** RPT-ATT-003 상태 기준: 주 5일↑ 🟢 / 3~4일 🟡 / 2일↓ 🟠. */
@ConfigurationProperties(prefix = "haemi.report")
public record ReportProperties(
        @DefaultValue("5") int goodThresholdDays,
        @DefaultValue("3") int normalThresholdDays,
        @DefaultValue("7") int weeklyWindowDays,
        @DefaultValue("4") int monthlyWindowWeeks,
        Pdf pdf
) {
    public ReportProperties {
        if (pdf == null) {
            pdf = new Pdf(true);
        }
    }

    /** 기존 호출부·테스트 호환용 (PDF 발송 기본 활성). */
    public ReportProperties(int goodThresholdDays, int normalThresholdDays,
                            int weeklyWindowDays, int monthlyWindowWeeks) {
        this(goodThresholdDays, normalThresholdDays, weeklyWindowDays, monthlyWindowWeeks, new Pdf(true));
    }

    /** 정기 PDF 발송 설정. */
    public record Pdf(
            @DefaultValue("true") boolean deliveryEnabled
    ) {}
}
