package com.memeboo2.haemi.guardian.report.presentation;

import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.guardian.report.application.ReportDeliveryService;
import com.memeboo2.haemi.guardian.report.application.ReportPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 정기 발송 로직을 수동 트리거하는 테스트 전용 엔드포인트. prod에서는 비활성. */
@Tag(name = "리포트 발송 (테스트)", description = "정기 리포트 발송 수동 트리거 — 비운영 전용")
@RestController
@RequiredArgsConstructor
@Profile("!prod")
public class ReportDispatchTestController {

    private final ReportDeliveryService deliveryService;

    @Operation(summary = "정기 리포트 발송 수동 트리거 (주간/월간)")
    @PostMapping("/api/v1/internal/report/dispatch")
    public ResponseEntity<ApiResponse<ReportDeliveryService.DispatchResult>> dispatch(
            @RequestParam(defaultValue = "WEEKLY") ReportPeriod period) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.dispatchAll(period)));
    }
}
