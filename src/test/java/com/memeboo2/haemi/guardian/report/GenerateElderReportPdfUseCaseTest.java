package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.report.application.GenerateElderReportPdfUseCase;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase;
import com.memeboo2.haemi.guardian.report.application.GetElderReportSummaryUseCase;
import com.memeboo2.haemi.guardian.report.application.ReportPdfPort;
import com.memeboo2.haemi.guardian.report.application.ReportPeriod;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GenerateElderReportPdfUseCaseTest {

    @Mock CareAccessQuery careAccessQuery;
    @Mock GetElderReportSummaryUseCase summaryUseCase;
    @Mock GetAttendanceDetailUseCase attendanceUseCase;
    @Mock ReportPdfPort pdfPort;
    @Mock HaemiClock clock;
    @InjectMocks GenerateElderReportPdfUseCase useCase;

    private final UUID guardianId = UUID.randomUUID();
    private final UUID elderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        given(clock.today()).willReturn(LocalDate.of(2026, 8, 27));
    }

    @Test
    void 인가된_보호자면_PDF를_생성하고_파일명을_구성한다() {
        GetElderReportSummaryUseCase.Summary summary = new GetElderReportSummaryUseCase.Summary(
                elderId, "김할머니", 80, "80대", 100L, true, 5, 7,
                ReportStatus.GOOD, 3, 10);
        GetAttendanceDetailUseCase.AttendanceDetail attendance = new GetAttendanceDetailUseCase.AttendanceDetail(
                List.of(), List.of(), 3, 10, ReportStatus.GOOD);

        given(summaryUseCase.execute(guardianId, elderId)).willReturn(summary);
        given(attendanceUseCase.execute(guardianId, elderId)).willReturn(attendance);
        given(careAccessQuery.roleOf(guardianId, elderId)).willReturn(GuardianRole.DAUGHTER);
        given(pdfPort.render(any(ReportPdfPort.ReportView.class))).willReturn(new byte[]{1, 2, 3});

        GenerateElderReportPdfUseCase.Result result = useCase.execute(guardianId, elderId, ReportPeriod.WEEKLY);

        verify(careAccessQuery).requireGuardianOf(guardianId, elderId);
        assertThat(result.pdf()).containsExactly(1, 2, 3);
        assertThat(result.filename()).contains("김할머니");
        assertThat(result.filename()).contains("20260827");
        assertThat(result.filename()).endsWith(".pdf");
    }

    @Test
    void 이름에_특수문자가_있으면_파일명에서_치환된다() {
        GetElderReportSummaryUseCase.Summary summary = new GetElderReportSummaryUseCase.Summary(
                elderId, "김/할*머니", 80, "80대", 100L, true, 5, 7,
                ReportStatus.GOOD, 3, 10);
        GetAttendanceDetailUseCase.AttendanceDetail attendance = new GetAttendanceDetailUseCase.AttendanceDetail(
                List.of(), List.of(), 3, 10, ReportStatus.GOOD);

        given(summaryUseCase.execute(guardianId, elderId)).willReturn(summary);
        given(attendanceUseCase.execute(guardianId, elderId)).willReturn(attendance);
        given(careAccessQuery.roleOf(guardianId, elderId)).willReturn(GuardianRole.SON);
        given(pdfPort.render(any(ReportPdfPort.ReportView.class))).willReturn(new byte[]{9});

        GenerateElderReportPdfUseCase.Result result = useCase.execute(guardianId, elderId, ReportPeriod.WEEKLY);

        assertThat(result.filename()).doesNotContain("/");
        assertThat(result.filename()).doesNotContain("*");
    }
}
