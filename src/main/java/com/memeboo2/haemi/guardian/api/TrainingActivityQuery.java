package com.memeboo2.haemi.guardian.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 보호자 홈 "오늘의 기록" 타임라인(#100 M2)에서 어르신의 인지 활동 완료 시각을 읽는 계약. */
public interface TrainingActivityQuery {

    /** 지정 날짜(KST)에 완료된 인지 훈련 세션들의 완료 시각. */
    List<CompletedSession> completedOn(UUID elderId, LocalDate date);

    record CompletedSession(Instant completedAt) {}
}
