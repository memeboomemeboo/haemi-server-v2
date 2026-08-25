package com.memeboo2.haemi.elder.training.presentation.dto;

import com.memeboo2.haemi.elder.training.application.TrainingSessionView;
import com.memeboo2.haemi.elder.training.application.TrainingQuestionView;
import com.memeboo2.haemi.elder.training.application.TrainingResultView;
import com.memeboo2.haemi.elder.training.domain.AnswerMode;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.guardian.api.AttendanceBadge;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TrainingSessionResponse(
        UUID id,
        SessionStatus status,
        QuestionType currentStep,
        Integer currentQuestionNumber,
        int totalQuestionCount,
        Instant startedAt,
        Instant completedAt,
        int inactivityReminderSeconds,
        String feedback,
        QuestionResponse currentQuestion,
        ResultResponse result
) {

    public static TrainingSessionResponse from(TrainingSessionView view) {
        return new TrainingSessionResponse(
                view.id(),
                view.status(),
                view.currentStep(),
                view.currentQuestionNumber(),
                view.totalQuestionCount(),
                view.startedAt(),
                view.completedAt(),
                view.inactivityReminderSeconds(),
                view.feedback(),
                QuestionResponse.from(view.currentQuestion()),
                ResultResponse.from(view.result())
        );
    }

    public static TrainingSessionResponse fromResult(TrainingResultView result) {
        return new TrainingSessionResponse(
                result.sessionId(), SessionStatus.COMPLETED, null, null, 0,
                null, result.completedAt(), 0, null, null, ResultResponse.from(result));
    }

    public record QuestionResponse(
            UUID id,
            int questionNumber,
            QuestionType questionType,
            AnswerMode answerMode,
            String prompt,
            String imageKey,
            List<String> options,
            String hint
    ) {
        private static QuestionResponse from(TrainingQuestionView view) {
            return view == null ? null : new QuestionResponse(
                    view.id(), view.questionNumber(), view.questionType(), view.answerMode(), view.prompt(),
                    view.imageKey(), view.options(), view.hint());
        }
    }

    public record ResultResponse(
            UUID sessionId,
            long participationSeconds,
            int delayedRecallSuccessCount,
            Instant completedAt,
            List<AttendanceBadge> unlockedBadges
    ) {
        private static ResultResponse from(TrainingResultView view) {
            return view == null ? null : new ResultResponse(
                    view.sessionId(), view.participationSeconds(), view.delayedRecallSuccessCount(), view.completedAt(),
                    view.unlockedBadges());
        }
    }
}
