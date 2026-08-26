package com.memeboo2.haemi.guardian.report.infrastructure;

import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.AttendanceDetail;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.DayMark;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.WeekBar;
import com.memeboo2.haemi.guardian.report.application.GetElderReportSummaryUseCase.Summary;
import com.memeboo2.haemi.guardian.report.application.ReportPdfPort;
import com.memeboo2.haemi.guardian.report.application.ReportPeriod;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CognitiveReportPdfRendererTest {

    private final CognitiveReportPdfRenderer renderer = new CognitiveReportPdfRenderer();

    @Test
    void 한글_리포트가_유효한_PDF로_렌더된다() {
        LocalDate today = LocalDate.of(2026, 8, 26);
        Summary summary = new Summary(
                UUID.randomUUID(), "김순자", 78, "70대", 123L, true,
                4, 7, ReportStatus.NORMAL, 3, 9);

        List<DayMark> days = List.of(
                new DayMark(today.minusDays(6), today.minusDays(6).getDayOfWeek(), true, true, false, false, false),
                new DayMark(today, today.getDayOfWeek(), false, false, false, false, false));
        List<WeekBar> weeks = List.of(
                new WeekBar(today.minusDays(27), today.minusDays(21), 5),
                new WeekBar(today.minusDays(6), today, 4));
        AttendanceDetail attendance = new AttendanceDetail(days, weeks, 3, 9, ReportStatus.NORMAL);

        ReportPdfPort.ReportView view = new ReportPdfPort.ReportView(
                ReportPeriod.WEEKLY, today, "딸", summary, attendance);

        byte[] pdf = renderer.render(view);

        assertThat(pdf).isNotEmpty();
        // PDF 매직 넘버 "%PDF"
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(1000);
    }
}
