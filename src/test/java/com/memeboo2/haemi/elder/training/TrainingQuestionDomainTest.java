package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.elder.training.domain.QuestionKind;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.TrainingQuestion;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainingQuestionDomainTest {

    private final UUID sessionId = UUID.randomUUID();
    /** 비-지남력 문항 채점에는 영향이 없는 임의의 채점 날짜. */
    private static final LocalDate ANY_DATE = LocalDate.of(2026, 8, 26);

    @Test
    void choice_모드에서_정답을_선택하면_true를_반환한다() {
        TrainingQuestion question = TrainingQuestion.choice(
                sessionId, 1, QuestionType.RECALL, QuestionKind.RECALL_TITLE,
                "질문", null, null, "정답", 0, null, List.of("정답", "오답1", "오답2"));

        assertThat(question.evaluate("정답", null, null, ANY_DATE)).isTrue();
    }

    @Test
    void choice_모드에서_오답을_선택하면_false를_반환한다() {
        TrainingQuestion question = TrainingQuestion.choice(
                sessionId, 1, QuestionType.RECALL, QuestionKind.RECALL_TITLE,
                "질문", null, null, "정답", 0, null, List.of("정답", "오답1", "오답2"));

        assertThat(question.evaluate("오답1", null, null, ANY_DATE)).isFalse();
    }

    @Test
    void choice_모드에서_selectedOption이_null이면_예외를_던진다() {
        TrainingQuestion question = TrainingQuestion.choice(
                sessionId, 1, QuestionType.RECALL, QuestionKind.RECALL_TITLE,
                "질문", null, null, "정답", 0, null, List.of("정답", "오답1"));

        assertThatThrownBy(() -> question.evaluate(null, null, null, ANY_DATE))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void choice_모드에서_보기에_없는_옵션이면_예외를_던진다() {
        TrainingQuestion question = TrainingQuestion.choice(
                sessionId, 1, QuestionType.RECALL, QuestionKind.RECALL_TITLE,
                "질문", null, null, "정답", 0, null, List.of("정답", "오답1"));

        assertThatThrownBy(() -> question.evaluate("존재하지않는보기", null, null, ANY_DATE))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void choice_모드_RECALL_YEAR에서_오차범위_이내면_true를_반환한다() {
        TrainingQuestion question = TrainingQuestion.choice(
                sessionId, 1, QuestionType.RECALL, QuestionKind.RECALL_YEAR,
                "질문", null, null, "2020", 2, null, List.of("2018", "2020", "2025"));

        assertThat(question.evaluate("2018", null, null, ANY_DATE)).isTrue();
    }

    @Test
    void choice_모드_RECALL_YEAR에서_오차범위_밖이면_false를_반환한다() {
        TrainingQuestion question = TrainingQuestion.choice(
                sessionId, 1, QuestionType.RECALL, QuestionKind.RECALL_YEAR,
                "질문", null, null, "2020", 1, null, List.of("2018", "2020", "2025"));

        assertThat(question.evaluate("2018", null, null, ANY_DATE)).isFalse();
    }

    @Test
    void textOrVoice_모드에서_텍스트_정답이면_true를_반환한다() {
        TrainingQuestion question = TrainingQuestion.textOrVoice(
                sessionId, 1, QuestionType.RECALL, QuestionKind.RECALL_TITLE,
                "질문", null, null, "정답", null);

        assertThat(question.evaluate(null, "이것은 정답 입니다", null, ANY_DATE)).isTrue();
    }

    @Test
    void textOrVoice_모드에서_음성_답변이면_null을_반환한다() {
        TrainingQuestion question = TrainingQuestion.textOrVoice(
                sessionId, 1, QuestionType.RECALL, QuestionKind.RECALL_TITLE,
                "질문", null, null, "정답", null);

        assertThat(question.evaluate(null, null, "voice-key", ANY_DATE)).isNull();
    }

    @Test
    void textOrVoice_모드에서_텍스트와_음성이_모두_없으면_예외를_던진다() {
        TrainingQuestion question = TrainingQuestion.textOrVoice(
                sessionId, 1, QuestionType.RECALL, QuestionKind.RECALL_TITLE,
                "질문", null, null, "정답", null);

        assertThatThrownBy(() -> question.evaluate(null, null, null, ANY_DATE))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void LANGUAGE_타입은_텍스트_답변이어도_null을_반환한다() {
        TrainingQuestion question = TrainingQuestion.textOrVoice(
                sessionId, 1, QuestionType.LANGUAGE, QuestionKind.LANGUAGE_NAMING,
                "질문", null, null, "정답", null);

        assertThat(question.evaluate(null, "아무 텍스트", null, ANY_DATE)).isNull();
    }

    @Test
    void choice_모드에서_다중_정답키_중_하나와_일치하면_true를_반환한다() {
        TrainingQuestion question = TrainingQuestion.choice(
                sessionId, 1, QuestionType.RECALL, QuestionKind.RECALL_TITLE,
                "질문", null, null, "정답1정답2", 0, null, List.of("정답1", "정답2", "오답"));

        assertThat(question.evaluate("정답2", null, null, ANY_DATE)).isTrue();
    }
}
