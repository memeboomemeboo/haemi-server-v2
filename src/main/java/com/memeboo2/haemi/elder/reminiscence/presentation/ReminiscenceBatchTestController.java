package com.memeboo2.haemi.elder.reminiscence.presentation;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.elder.reminiscence.application.DailyReminiscenceBatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 매일 08:00 배치를 수동 실행하는 테스트 전용 엔드포인트. prod에서는 비활성. */
@Tag(name = "AI 회상 배치 (테스트)", description = "개인화 회상 콘텐츠 배치 수동 트리거 — 비운영 전용")
@RestController
@RequiredArgsConstructor
@Profile("!prod")
public class ReminiscenceBatchTestController {

    private final DailyReminiscenceBatch batch;
    private final HaemiClock clock;

    @Operation(summary = "개인화 회상 콘텐츠 배치 수동 실행")
    @PostMapping("/api/v1/internal/ai/reminiscence/run")
    public ResponseEntity<ApiResponse<DailyReminiscenceBatch.BatchResult>> run() {
        return ResponseEntity.ok(ApiResponse.ok(batch.generateForAll(clock.today())));
    }
}
