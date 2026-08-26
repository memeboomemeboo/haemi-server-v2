package com.memeboo2.haemi.guardian.report.application;

/** 리포트 발송 주기. 주간=최근 7일, 월간=최근 4주. */
public enum ReportPeriod {
    WEEKLY("주간"),
    MONTHLY("월간");

    private final String label;

    ReportPeriod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
