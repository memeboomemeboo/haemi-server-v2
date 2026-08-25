package com.memeboo2.haemi.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** CIST 세션 완료 사실. 출석과 이후 리포트가 멱등 읽기 모델을 만드는 원천 이벤트다. */
public record TrainingSessionCompleted(
        @JsonProperty("s") UUID trainingSessionId,
        @JsonProperty("e") UUID elderId,
        @JsonProperty("d") LocalDate completedDate,
        @JsonProperty("t") Instant completedAt,
        @JsonProperty("p") long participationSeconds,
        @JsonProperty("r") int delayedRecallSuccessCount
) {}
