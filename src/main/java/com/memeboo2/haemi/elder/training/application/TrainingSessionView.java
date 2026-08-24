package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;

import java.time.Instant;
import java.util.UUID;

public record TrainingSessionView(
        UUID id,
        SessionStatus status,
        QuestionType currentStep,
        Integer currentQuestionNumber,
        int totalQuestionCount,
        Instant startedAt,
        Instant completedAt
) {

    public static TrainingSessionView from(TrainingSession session) {
        return new TrainingSessionView(
                session.getId(),
                session.getStatus(),
                session.getCurrentStep(),
                session.getCurrentQuestionNumber(),
                TrainingSession.TOTAL_QUESTION_COUNT,
                session.getStartedAt(),
                session.getCompletedAt()
        );
    }
}
