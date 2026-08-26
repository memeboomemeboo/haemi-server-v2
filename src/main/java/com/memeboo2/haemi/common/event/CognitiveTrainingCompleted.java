package com.memeboo2.haemi.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CIST 완료 시 guardian/report에만 전달하는 영역별 자동 채점 집계 계약.
 *
 * <p>Spring Modulith JPA outbox의 직렬화 열(255자)에 맞추기 위해 JSON 필드명은 짧게 고정한다.
 * 답변 원문·개별 답변·점수는 전송하지 않고, 영역별 채점 가능 수와 정답 수만 포함한다.</p>
 */
@Externalized
public record CognitiveTrainingCompleted(
        @JsonProperty("e") UUID elderId,
        @JsonProperty("s") UUID sessionId,
        @JsonProperty("d") LocalDate sessionDate,
        @JsonProperty("r") List<CognitiveAreaResult> cognitiveAreaResults
) {

    public CognitiveTrainingCompleted {
        cognitiveAreaResults = List.copyOf(cognitiveAreaResults);
    }

    /** 리포트 읽기 모델이 멱등 적재할 최소 영역별 집계. */
    public record CognitiveAreaResult(
            @JsonProperty("a") String area,
            @JsonProperty("s") int scoredAnswerCount,
            @JsonProperty("c") int correctAnswerCount
    ) {

        public CognitiveAreaResult {
            if (area == null || area.isBlank()) {
                throw new IllegalArgumentException("인지 영역은 비어 있을 수 없습니다.");
            }
            if (scoredAnswerCount < 0 || correctAnswerCount < 0 || correctAnswerCount > scoredAnswerCount) {
                throw new IllegalArgumentException("인지 영역 정답 집계가 올바르지 않습니다.");
            }
        }
    }
}
