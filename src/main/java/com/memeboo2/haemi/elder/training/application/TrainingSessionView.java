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
        Instant completedAt,
        int inactivityReminderSeconds,
        String feedback,
        TrainingQuestionView currentQuestion,
        TrainingResultView result
) {

    public static TrainingSessionView from(
            TrainingSession session,
            int totalQuestionCount,
            int inactivityReminderSeconds,
            String feedback,
            TrainingQuestionView currentQuestion,
            TrainingResultView result
    ) {
        return new TrainingSessionView(
                session.getId(),
                session.getStatus(),
                session.getCurrentStep(),
                session.getCurrentQuestionNumber(),
                totalQuestionCount,
                session.getStartedAt(),
                session.getCompletedAt(),
                inactivityReminderSeconds,
                feedback,
                currentQuestion,
                result
        );
    }
}
