package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.elder.training.domain.AnswerMode;
import com.memeboo2.haemi.elder.training.domain.QuestionKind;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingQuestion;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CIST 세션의 완료 경계와 참여형 언어 응답의 도메인 규칙을 검증한다. */
class TrainingSessionEdgeCaseTest {

    @Test
    void 마지막인_10번_문항에서만_세션을_완료하고_이전_문항의_재전송은_막는다() {
        TrainingSession session = TrainingSession.start(
                UUID.randomUUID(), Instant.parse("2026-08-25T00:00:00Z"), LocalDate.of(2026, 8, 25));

        for (int number = 1; number < 10; number++) {
            session.completeCurrentQuestion(
                    session.getId(), session.getCurrentStep(), number, nextType(number), false,
                    Instant.parse("2026-08-25T00:00:00Z"));
        }

        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.getCurrentQuestionNumber()).isEqualTo(10);
        assertThatThrownBy(() -> session.completeCurrentQuestion(
                session.getId(), QuestionType.DELAYED_RECALL, 9, QuestionType.DELAYED_RECALL, false, Instant.now()))
                .isInstanceOf(DomainException.class);

        session.completeCurrentQuestion(
                session.getId(), QuestionType.DELAYED_RECALL, 10, null, true, Instant.parse("2026-08-25T00:05:00Z"));

        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getCurrentQuestionNumber()).isNull();
    }

    @Test
    void 언어_문항은_텍스트나_음성_참여를_요구하지만_정확성으로_판단하지_않는다() {
        TrainingQuestion question = TrainingQuestion.textOrVoice(
                UUID.randomUUID(), 7, QuestionType.LANGUAGE, QuestionKind.LANGUAGE_DESCRIPTION,
                "사진을 설명해 주세요.", "memory/example.jpg", null, "어린 시절", null);

        assertThat(question.getAnswerMode()).isEqualTo(AnswerMode.TEXT_OR_VOICE);
        assertThat(question.evaluate(null, "가족과 함께 찍은 사진이에요.", null)).isNull();
        assertThatThrownBy(() -> question.evaluate(null, " ", null)).isInstanceOf(DomainException.class);
    }

    @Test
    void 연도_문항은_레벨별_허용_오차_안에서만_수용한다() {
        TrainingQuestion question = TrainingQuestion.choice(
                UUID.randomUUID(), 4, QuestionType.RECALL, QuestionKind.RECALL_YEAR,
                "몇 년도쯤인가요?", "content/example.jpg", null, "1970", 10, null,
                List.of("1970", "1980", "1990"));

        assertThat(question.evaluate("1980", null, null)).isTrue();
        assertThat(question.evaluate("1990", null, null)).isFalse();
    }

    private QuestionType nextType(int currentNumber) {
        return switch (currentNumber) {
            case 1, 2 -> QuestionType.ORIENTATION;
            case 3, 4, 5 -> QuestionType.RECALL;
            case 6, 7 -> QuestionType.LANGUAGE;
            default -> QuestionType.DELAYED_RECALL;
        };
    }
}
