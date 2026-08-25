package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.guardian.api.AttendanceBadge;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** CIST-TRN-006의 참여 중심 결과. 내부 점수·정답률은 포함하지 않는다. */
public record TrainingResultView(
        UUID sessionId,
        boolean completed,
        long participationSeconds,
        int delayedRecallSuccessCount,
        Instant completedAt,
        List<AttendanceBadge> unlockedBadges
) {}
