package com.memeboo2.haemi.guardian.report.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase;
import com.memeboo2.haemi.guardian.report.application.GetElderReportListUseCase;
import com.memeboo2.haemi.guardian.report.application.GetElderReportSummaryUseCase;
import com.memeboo2.haemi.guardian.report.presentation.dto.AttendanceDetailResponse;
import com.memeboo2.haemi.guardian.report.presentation.dto.ElderReportCardResponse;
import com.memeboo2.haemi.guardian.report.presentation.dto.ElderReportSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "리포트 (보호자)", description = "어르신 출석·참여 리포트. 어르신 본인은 접근 불가 (R9)")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final GetElderReportListUseCase getElderReportListUseCase;
    private final GetElderReportSummaryUseCase getElderReportSummaryUseCase;
    private final GetAttendanceDetailUseCase getAttendanceDetailUseCase;

    @Operation(summary = "어르신 리포트 목록 (RPT-LST-001)",
            description = "관찰필요(🟠) → 보통(🟡) → 좋음(🟢) 순 정렬. 어르신 간 비교·순위는 제공하지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/api/v1/guardian/report/elders")
    public ResponseEntity<ApiResponse<List<ElderReportCardResponse>>> list(
            @RequestAttribute UUID guardianId) {
        List<ElderReportCardResponse> result = getElderReportListUseCase.execute(guardianId)
                .stream().map(ElderReportCardResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "어르신 요약 카드 (RPT-LST-002)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인가 실패 — CARE_ACCESS_DENIED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 어르신")
    })
    @GetMapping("/api/v1/guardian/elders/{elderId}/report/summary")
    public ResponseEntity<ApiResponse<ElderReportSummaryResponse>> summary(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId) {
        var summary = getElderReportSummaryUseCase.execute(guardianId, elderId);
        return ResponseEntity.ok(ApiResponse.ok(ElderReportSummaryResponse.from(summary)));
    }

    @Operation(summary = "출석 및 참여 현황 (RPT-ATT-003)", description = "최근 7일 요일별 완료 점 + 최근 4주 막대.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인가 실패 — CARE_ACCESS_DENIED")
    })
    @GetMapping("/api/v1/guardian/elders/{elderId}/report/attendance")
    public ResponseEntity<ApiResponse<AttendanceDetailResponse>> attendance(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId) {
        var detail = getAttendanceDetailUseCase.execute(guardianId, elderId);
        return ResponseEntity.ok(ApiResponse.ok(AttendanceDetailResponse.from(detail)));
    }
}
