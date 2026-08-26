package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.AttendanceDetail;
import com.memeboo2.haemi.guardian.report.application.GetElderReportSummaryUseCase.Summary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** 어르신 인지 리포트를 PDF 바이트로 생성한다 (다운로드·이메일 공용). */
@Service
@RequiredArgsConstructor
public class GenerateElderReportPdfUseCase {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CareAccessQuery careAccessQuery;
    private final GetElderReportSummaryUseCase summaryUseCase;
    private final GetAttendanceDetailUseCase attendanceUseCase;
    private final ReportPdfPort pdfPort;
    private final HaemiClock clock;

    @Transactional(readOnly = true)
    public Result execute(UUID guardianId, UUID elderId, ReportPeriod period) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);

        Summary summary = summaryUseCase.execute(guardianId, elderId);
        AttendanceDetail attendance = attendanceUseCase.execute(guardianId, elderId);
        String roleLabel = careAccessQuery.roleOf(guardianId, elderId).getLabel();

        ReportPdfPort.ReportView view = new ReportPdfPort.ReportView(
                period, clock.today(), roleLabel, summary, attendance);
        byte[] pdf = pdfPort.render(view);

        String filename = "인지리포트_%s_%s_%s.pdf".formatted(
                sanitize(summary.name()), period.label(), clock.today().format(FILE_DATE));
        return new Result(filename, pdf);
    }

    private String sanitize(String name) {
        return name == null ? "어르신" : name.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
    }

    public record Result(String filename, byte[] pdf) {}
}
