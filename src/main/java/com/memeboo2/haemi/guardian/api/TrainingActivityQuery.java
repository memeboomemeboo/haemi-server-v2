package com.memeboo2.haemi.guardian.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 보호자 홈 "오늘의 기록" 타임라인(#100 M2)에서 어르신의 인지 활동 완료 시각을 읽는 계약. */
public interface TrainingActivityQuery {

    /** 지정 날짜(KST)에 완료된 인지 훈련 세션의 타임라인 표시 정보. */
    List<CompletedSession> completedOn(UUID elderId, LocalDate date);

    record CompletedSession(Instant completedAt, int durationMinutes, int accuracy) {
        public CompletedSession(Instant completedAt) {
            this(completedAt, 0, 0);
        }
    }
}
