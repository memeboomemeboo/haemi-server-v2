package com.memeboo2.haemi.guardian.report.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.report.application.GetAttendanceDetailUseCase;
import com.memeboo2.haemi.guardian.report.application.GetCognitiveStatusUseCase;
import com.memeboo2.haemi.guardian.report.application.GetElderReportListUseCase;
import com.memeboo2.haemi.guardian.report.application.GetElderReportSummaryUseCase;
import com.memeboo2.haemi.guardian.report.application.GetSupportGuideUseCase;
import com.memeboo2.haemi.guardian.report.application.GetWeeklyHighlightUseCase;
import com.memeboo2.haemi.guardian.report.application.UpdateWeeklyHighlightUseCase;
import com.memeboo2.haemi.guardian.report.presentation.dto.AttendanceDetailResponse;
import com.memeboo2.haemi.guardian.report.presentation.dto.CognitiveStatusResponse;
import com.memeboo2.haemi.guardian.report.presentation.dto.ElderReportCardResponse;
import com.memeboo2.haemi.guardian.report.presentation.dto.ElderReportSummaryResponse;
import com.memeboo2.haemi.guardian.report.presentation.dto.SupportGuideResponse;
import com.memeboo2.haemi.guardian.report.presentation.dto.WeeklyHighlightResponse;
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
    private final GetCognitiveStatusUseCase getCognitiveStatusUseCase;
    private final GetWeeklyHighlightUseCase getWeeklyHighlightUseCase;
    private final UpdateWeeklyHighlightUseCase updateWeeklyHighlightUseCase;
    private final GetSupportGuideUseCase getSupportGuideUseCase;

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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "인가 실패 — CARE_ACCESS_DENIED. requireGuardianOf가 존재 여부보다 먼저 검사돼 "
                            + "존재하지 않는 elderId도 링크가 없으므로 403으로 응답한다 (404는 도달하지 않음)")
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

    @Operation(summary = "인지 영역별 상태 (RPT-ATT-004)",
            description = "정답률·점수 대신 각 인지 영역의 3색 상태와 관찰 신호만 제공한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인가 실패 — CARE_ACCESS_DENIED")
    })
    @GetMapping("/api/v1/guardian/elders/{elderId}/report/cognitive-status")
    public ResponseEntity<ApiResponse<CognitiveStatusResponse>> cognitiveStatus(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId) {
        var status = getCognitiveStatusUseCase.cognitiveStatus(guardianId, elderId);
        return ResponseEntity.ok(ApiResponse.ok(CognitiveStatusResponse.from(status)));
    }

    @Operation(summary = "이번 주 하이라이트 (RPT-ATT-005)",
            description = "잘한 점을 먼저 전하고, 필요한 경우 관찰 신호를 1~3줄로 제공한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인가 실패 — CARE_ACCESS_DENIED")
    })
    @GetMapping("/api/v1/guardian/elders/{elderId}/report/highlight")
    public ResponseEntity<ApiResponse<WeeklyHighlightResponse>> weeklyHighlight(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId) {
        var highlight = getWeeklyHighlightUseCase.execute(guardianId, elderId);
        return ResponseEntity.ok(ApiResponse.ok(WeeklyHighlightResponse.from(highlight)));
    }

    public record UpdateHighlightRequest(
            @jakarta.validation.constraints.NotEmpty List<@jakarta.validation.constraints.NotBlank String> lines) {}

    @Operation(summary = "이번 주 하이라이트 편집 (#100 M5)",
            description = "보호자가 이번 주 하이라이트 문구를 직접 수정한다. 이후 조회는 편집된 문구를 반환한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "빈 문구 — INVALID_INPUT"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인가 실패 — CARE_ACCESS_DENIED")
    })
    @PatchMapping("/api/v1/guardian/elders/{elderId}/report/highlight")
    public ResponseEntity<ApiResponse<WeeklyHighlightResponse>> updateWeeklyHighlight(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId,
            @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid UpdateHighlightRequest req) {
        var highlight = updateWeeklyHighlightUseCase.execute(guardianId, elderId, req.lines());
        return ResponseEntity.ok(ApiResponse.ok(WeeklyHighlightResponse.from(highlight)));
    }

    @Operation(summary = "서포트 가이드 (RPT-ATT-006)",
            description = "인지·참여 상태를 보호자가 바로 할 수 있는 행동으로 번역한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "인가 실패 — CARE_ACCESS_DENIED")
    })
    @GetMapping("/api/v1/guardian/elders/{elderId}/report/support-guide")
    public ResponseEntity<ApiResponse<SupportGuideResponse>> supportGuide(
            @RequestAttribute UUID guardianId,
            @PathVariable UUID elderId) {
        var guide = getSupportGuideUseCase.execute(guardianId, elderId);
        return ResponseEntity.ok(ApiResponse.ok(SupportGuideResponse.from(guide)));
    }
}
