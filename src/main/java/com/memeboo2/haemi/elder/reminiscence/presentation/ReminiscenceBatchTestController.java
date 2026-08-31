package com.memeboo2.haemi.elder.reminiscence.presentation;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.common.web.ApiResponse;
import com.memeboo2.haemi.elder.reminiscence.application.DailyReminiscenceBatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매일 08:00 배치를 수동 실행하는 테스트 전용 엔드포인트.
 * <p>인증만 되면 아무 역할이나 전체 어르신 규모의 Gemini 배치를 호출할 수 있고(비용 증폭),
 * {@code @Profile("!prod")}는 프로필이 비면 활성화되는 쪽으로 실패한다. 이를 막기 위해
 * 비운영 프로필 <b>그리고</b> 명시적 opt-in 속성이 모두 있을 때만 빈으로 등록한다(기본 비활성 = fail-closed).
 * 속성을 켜는 운영자는 신뢰된 환경에서만 켠다는 전제다. (#145)
 */
@Tag(name = "AI 회상 배치 (테스트)", description = "개인화 회상 콘텐츠 배치 수동 트리거 — 비운영 전용")
@RestController
@RequiredArgsConstructor
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "haemi.ai.reminiscence", name = "manual-trigger-enabled", havingValue = "true")
public class ReminiscenceBatchTestController {

    private final DailyReminiscenceBatch batch;
    private final HaemiClock clock;

    @Operation(summary = "개인화 회상 콘텐츠 배치 수동 실행")
    @PostMapping("/api/v1/internal/ai/reminiscence/run")
    public ResponseEntity<ApiResponse<DailyReminiscenceBatch.BatchResult>> run() {
        return ResponseEntity.ok(ApiResponse.ok(batch.generateForAll(clock.today())));
    }
}
