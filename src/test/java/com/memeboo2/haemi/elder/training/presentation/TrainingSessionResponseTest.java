package com.memeboo2.haemi.elder.training.presentation;

import com.memeboo2.haemi.elder.training.application.TrainingQuestionView;
import com.memeboo2.haemi.elder.training.application.TrainingResultView;
import com.memeboo2.haemi.elder.training.application.TrainingSessionView;
import com.memeboo2.haemi.elder.training.domain.AnswerMode;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.presentation.dto.TrainingSessionResponse;
import com.memeboo2.haemi.guardian.api.AttendanceBadge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingSessionResponseTest {

    @Test
    @DisplayName("TrainingSessionView로부터 세션과 현재 질문, 결과를 모두 매핑한다")
    void from_전체_필드를_매핑한다() {
        UUID sessionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-08-01T00:00:00Z");
        Instant completedAt = Instant.parse("2026-08-01T00:10:00Z");

        TrainingQuestionView question = new TrainingQuestionView(
                questionId, 1, QuestionType.RECALL, AnswerMode.CHOICE,
                "질문 프롬프트", "image-key", List.of("보기1", "보기2"), "힌트");

        TrainingResultView result = new TrainingResultView(
                sessionId, true, 120L, 3, completedAt, List.of(AttendanceBadge.DAYS_7));

        TrainingSessionView view = new TrainingSessionView(
                sessionId, SessionStatus.IN_PROGRESS, QuestionType.RECALL, 1, 5,
                startedAt, completedAt, 30, "피드백", question, result);

        TrainingSessionResponse response = TrainingSessionResponse.from(view);

        assertThat(response.id()).isEqualTo(sessionId);
        assertThat(response.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(response.currentStep()).isEqualTo(QuestionType.RECALL);
        assertThat(response.currentQuestionNumber()).isEqualTo(1);
        assertThat(response.totalQuestionCount()).isEqualTo(5);
        assertThat(response.startedAt()).isEqualTo(startedAt);
        assertThat(response.completedAt()).isEqualTo(completedAt);
        assertThat(response.inactivityReminderSeconds()).isEqualTo(30);
        assertThat(response.feedback()).isEqualTo("피드백");

        assertThat(response.currentQuestion().id()).isEqualTo(questionId);
        assertThat(response.currentQuestion().questionNumber()).isEqualTo(1);
        assertThat(response.currentQuestion().questionType()).isEqualTo(QuestionType.RECALL);
        assertThat(response.currentQuestion().answerMode()).isEqualTo(AnswerMode.CHOICE);
        assertThat(response.currentQuestion().prompt()).isEqualTo("질문 프롬프트");
        assertThat(response.currentQuestion().imageKey()).isEqualTo("image-key");
        assertThat(response.currentQuestion().options()).containsExactly("보기1", "보기2");
        assertThat(response.currentQuestion().hint()).isEqualTo("힌트");

        assertThat(response.result().sessionId()).isEqualTo(sessionId);
        assertThat(response.result().participationSeconds()).isEqualTo(120L);
        assertThat(response.result().delayedRecallSuccessCount()).isEqualTo(3);
        assertThat(response.result().completedAt()).isEqualTo(completedAt);
        assertThat(response.result().unlockedBadges()).containsExactly(AttendanceBadge.DAYS_7);
    }

    @Test
    @DisplayName("현재 질문과 결과가 없으면 currentQuestion과 result는 null이다")
    void from_질문과_결과가_없으면_null이다() {
        TrainingSessionView view = new TrainingSessionView(
                UUID.randomUUID(), SessionStatus.IN_PROGRESS, QuestionType.RECALL, null, 5,
                Instant.now(), null, 30, null, null, null);

        TrainingSessionResponse response = TrainingSessionResponse.from(view);

        assertThat(response.currentQuestion()).isNull();
        assertThat(response.result()).isNull();
    }

    @Test
    @DisplayName("fromResult는 완료 상태의 세션 응답을 생성하고 결과를 채운다")
    void fromResult_완료된_세션_응답을_생성한다() {
        UUID sessionId = UUID.randomUUID();
        Instant completedAt = Instant.parse("2026-08-01T00:10:00Z");
        TrainingResultView result = new TrainingResultView(
                sessionId, true, 200L, 5, completedAt, List.of());

        TrainingSessionResponse response = TrainingSessionResponse.fromResult(result);

        assertThat(response.id()).isEqualTo(sessionId);
        assertThat(response.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(response.currentStep()).isNull();
        assertThat(response.currentQuestionNumber()).isNull();
        assertThat(response.totalQuestionCount()).isZero();
        assertThat(response.startedAt()).isNull();
        assertThat(response.completedAt()).isEqualTo(completedAt);
        assertThat(response.inactivityReminderSeconds()).isZero();
        assertThat(response.feedback()).isNull();
        assertThat(response.currentQuestion()).isNull();
        assertThat(response.result().sessionId()).isEqualTo(sessionId);
        assertThat(response.result().participationSeconds()).isEqualTo(200L);
    }
}
