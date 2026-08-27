package com.memeboo2.haemi.guardian.report.infrastructure;

import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.AttendanceDetail;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.DayMark;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.WeekBar;
import com.memeboo2.haemi.guardian.report.application.GetElderReportSummaryUseCase.Summary;
import com.memeboo2.haemi.guardian.report.application.ReportPdfPort.ReportView;
import com.memeboo2.haemi.guardian.report.application.ReportPeriod;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** CognitiveReportPdfRenderer의 PDF 렌더링 단위 테스트. openhtmltopdf/폰트가 클래스패스에 있어야 통과한다. */
class CognitiveReportPdfRendererUnitTest {

    private final CognitiveReportPdfRenderer renderer = new CognitiveReportPdfRenderer();

    @Test
    void 정상_데이터로_PDF_바이트를_생성한다() {
        ReportView view = buildView(ReportStatus.GOOD, "김할머니", 82, "80대", 30);

        byte[] pdf = renderer.render(view);

        assertThat(pdf).isNotEmpty();
        assertThat(pdf[0]).isEqualTo((byte) '%');
        assertThat(pdf[1]).isEqualTo((byte) 'P');
        assertThat(pdf[2]).isEqualTo((byte) 'D');
        assertThat(pdf[3]).isEqualTo((byte) 'F');
    }

    @Test
    void 나이와_세대_정보가_없어도_PDF를_생성한다() {
        ReportView view = buildView(ReportStatus.WATCH, "이할아버지", null, null, 10);

        byte[] pdf = renderer.render(view);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void 이름에_HTML_특수문자가_있어도_이스케이프되어_렌더링된다() {
        ReportView view = buildView(ReportStatus.NORMAL, "<b>박</b>&\"할머니\"", 75, "70대", 5);

        byte[] pdf = renderer.render(view);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void 세_가지_상태값_모두_정상_렌더링된다() {
        for (ReportStatus status : ReportStatus.values()) {
            ReportView view = buildView(status, "테스트", 60, "60대", 1);
            byte[] pdf = renderer.render(view);
            assertThat(pdf).isNotEmpty();
        }
    }

    @Test
    void 참여_기록이_없는_빈_주간에도_렌더링된다() {
        LocalDate today = LocalDate.of(2026, 8, 27);
        List<DayMark> days = List.of(absentDay(today));
        List<WeekBar> weeks = List.of(new WeekBar(today.minusDays(6), today, 0));
        AttendanceDetail attendance = new AttendanceDetail(days, weeks, 0, 0, ReportStatus.WATCH);
        Summary summary = new Summary(UUID.randomUUID(), "무기록", null, null, 0, false, 0, 5,
                ReportStatus.WATCH, 0, 0);
        ReportView view = new ReportView(ReportPeriod.WEEKLY, today, "자녀", summary, attendance);

        byte[] pdf = renderer.render(view);

        assertThat(pdf).isNotEmpty();
    }

    private DayMark absentDay(LocalDate date) {
        return new DayMark(date, date.getDayOfWeek(), false, false, false, false, false);
    }

    private ReportView buildView(ReportStatus status, String name, Integer age, String generation, long daysTogether) {
        LocalDate today = LocalDate.of(2026, 8, 27);
        List<DayMark> days = List.of(
                absentDay(today.minusDays(6)),
                absentDay(today.minusDays(5)),
                absentDay(today.minusDays(4)),
                absentDay(today.minusDays(3)),
                absentDay(today.minusDays(2)),
                absentDay(today.minusDays(1)),
                absentDay(today)
        );
        List<WeekBar> weeks = List.of(
                new WeekBar(today.minusDays(27), today.minusDays(21), 3),
                new WeekBar(today.minusDays(20), today.minusDays(14), 5),
                new WeekBar(today.minusDays(13), today.minusDays(7), 2),
                new WeekBar(today.minusDays(6), today, 4)
        );
        AttendanceDetail attendance = new AttendanceDetail(days, weeks, 3, 7, status);
        Summary summary = new Summary(UUID.randomUUID(), name, age, generation, daysTogether, true, 4, 5,
                status, 3, 7);
        return new ReportView(ReportPeriod.WEEKLY, today, "자녀", summary, attendance);
    }
}
