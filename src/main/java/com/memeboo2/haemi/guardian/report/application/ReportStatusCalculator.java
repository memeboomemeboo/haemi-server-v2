package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import org.springframework.stereotype.Component;

/** 최근 7일 참여일 수 → 3색 상태 (RPT-ATT-003). */
@Component
public class ReportStatusCalculator {

    private final ReportProperties props;

    public ReportStatusCalculator(ReportProperties props) {
        this.props = props;
    }

    public ReportStatus fromWeeklyParticipationDays(int days) {
        if (days >= props.goodThresholdDays()) {
            return ReportStatus.GOOD;
        }
        if (days >= props.normalThresholdDays()) {
            return ReportStatus.NORMAL;
        }
        return ReportStatus.WATCH;
    }
}
