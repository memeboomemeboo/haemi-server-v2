package com.memeboo2.haemi.common.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** 인지 훈련 완료로 확정된 일자별 참여 기록이다. */
public record AttendanceRecorded(
        UUID trainingSessionId,
        UUID elderId,
        LocalDate participationDate,
        Instant recordedAt
) {}
