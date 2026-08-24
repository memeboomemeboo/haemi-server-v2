package com.memeboo2.haemi.common.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** CIST 세션 완료 사실. 출석과 리포트가 멱등 읽기 모델을 만드는 원천 이벤트다. */
public record TrainingSessionCompleted(
        UUID trainingSessionId,
        UUID elderId,
        LocalDate sessionDate,
        LocalDate completedDate,
        Instant completedAt
) {}
