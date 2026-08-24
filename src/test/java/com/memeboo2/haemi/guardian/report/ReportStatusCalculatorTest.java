package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.guardian.report.application.ReportProperties;
import com.memeboo2.haemi.guardian.report.application.ReportStatusCalculator;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportStatusCalculatorTest {

    private final ReportStatusCalculator calculator = new ReportStatusCalculator(new ReportProperties(5, 3, 7, 4));

    @Test
    void 주5일_이상이면_GOOD() {
        assertThat(calculator.fromWeeklyParticipationDays(5)).isEqualTo(ReportStatus.GOOD);
        assertThat(calculator.fromWeeklyParticipationDays(7)).isEqualTo(ReportStatus.GOOD);
    }

    @Test
    void 주3_4일이면_NORMAL() {
        assertThat(calculator.fromWeeklyParticipationDays(3)).isEqualTo(ReportStatus.NORMAL);
        assertThat(calculator.fromWeeklyParticipationDays(4)).isEqualTo(ReportStatus.NORMAL);
    }

    @Test
    void 주2일_이하면_WATCH() {
        assertThat(calculator.fromWeeklyParticipationDays(2)).isEqualTo(ReportStatus.WATCH);
        assertThat(calculator.fromWeeklyParticipationDays(0)).isEqualTo(ReportStatus.WATCH);
    }
}
