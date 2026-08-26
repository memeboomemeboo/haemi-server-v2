package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase.AttendanceDetail;
import com.memeboo2.haemi.guardian.report.application.GetElderReportSummaryUseCase.Summary;

import java.time.LocalDate;

/** 리포트 데이터를 한글 임베드 PDF 바이트로 렌더링한다. */
public interface ReportPdfPort {

    byte[] render(ReportView view);

    record ReportView(
            ReportPeriod period,
            LocalDate generatedOn,
            String guardianRoleLabel,
            Summary summary,
            AttendanceDetail attendance
    ) {}
}
