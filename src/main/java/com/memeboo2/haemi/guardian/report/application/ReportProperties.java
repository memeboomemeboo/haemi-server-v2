package com.memeboo2.haemi.guardian.report.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** RPT-ATT-003 상태 기준: 주 5일↑ 🟢 / 3~4일 🟡 / 2일↓ 🟠. */
@ConfigurationProperties(prefix = "haemi.report")
public record ReportProperties(
        @DefaultValue("5") int goodThresholdDays,
        @DefaultValue("3") int normalThresholdDays,
        @DefaultValue("7") int weeklyWindowDays,
        @DefaultValue("4") int monthlyWindowWeeks,
        @DefaultValue("70") int cognitiveGoodAccuracyPercent,
        @DefaultValue("40") int cognitiveNormalAccuracyPercent,
        @DefaultValue("7") int cognitiveRecentWindowDays,
        @DefaultValue("4") int cognitiveTrendWindowWeeks
) {

    /** 보조 생성자가 있어도 Spring은 이 canonical 생성자로 설정을 바인딩한다. */
    @ConstructorBinding
    public ReportProperties {}

    /** 기존 RPT-ATT-003 테스트·생성부와의 호환 생성자. */
    public ReportProperties(int goodThresholdDays, int normalThresholdDays, int weeklyWindowDays, int monthlyWindowWeeks) {
        this(goodThresholdDays, normalThresholdDays, weeklyWindowDays, monthlyWindowWeeks, 70, 40, 7, 4);
    }
}
