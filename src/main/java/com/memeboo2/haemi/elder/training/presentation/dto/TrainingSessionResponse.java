package com.memeboo2.haemi.elder.training.presentation.dto;

import com.memeboo2.haemi.elder.training.application.TrainingSessionView;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public record TrainingSessionResponse(
        UUID id,
        SessionStatus status,
        QuestionType currentStep,
        Integer currentQuestionNumber,
        int totalQuestionCount,
        Instant startedAt,
        Instant completedAt
) {

    public static TrainingSessionResponse from(TrainingSessionView view) {
        return new TrainingSessionResponse(
                view.id(),
                view.status(),
                view.currentStep(),
                view.currentQuestionNumber(),
                view.totalQuestionCount(),
                view.startedAt(),
                view.completedAt()
        );
    }
}
