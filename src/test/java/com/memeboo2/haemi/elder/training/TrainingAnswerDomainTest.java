package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.TrainingAnswer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** TrainingAnswer의 record 팩토리를 검증한다. */
class TrainingAnswerDomainTest {

    private static final Instant ANSWERED_AT = Instant.parse("2026-08-27T00:00:00Z");

    @Test
    void record은_전달받은_값으로_답변을_생성한다() {
        UUID sessionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();

        TrainingAnswer answer = TrainingAnswer.record(
                sessionId, questionId, elderId, 1, QuestionType.ORIENTATION,
                "1", null, null, true, ANSWERED_AT);

        assertThat(answer.getId()).isNotNull();
        assertThat(answer.getSessionId()).isEqualTo(sessionId);
        assertThat(answer.getQuestionId()).isEqualTo(questionId);
        assertThat(answer.getElderId()).isEqualTo(elderId);
        assertThat(answer.getQuestionNumber()).isEqualTo(1);
        assertThat(answer.getQuestionType()).isEqualTo(QuestionType.ORIENTATION);
        assertThat(answer.getSelectedOption()).isEqualTo("1");
        assertThat(answer.getTextAnswer()).isNull();
        assertThat(answer.getVoiceMediaKey()).isNull();
        assertThat(answer.getCorrect()).isTrue();
        assertThat(answer.getAnsweredAt()).isEqualTo(ANSWERED_AT);
    }

    @Test
    void record은_호출할_때마다_새로운_id를_부여한다() {
        UUID sessionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();

        TrainingAnswer first = TrainingAnswer.record(
                sessionId, questionId, elderId, 1, QuestionType.ORIENTATION,
                "1", null, null, true, ANSWERED_AT);
        TrainingAnswer second = TrainingAnswer.record(
                sessionId, questionId, elderId, 1, QuestionType.ORIENTATION,
                "1", null, null, true, ANSWERED_AT);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void 텍스트_답변을_기록할_수_있다() {
        TrainingAnswer answer = TrainingAnswer.record(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2, QuestionType.RECALL,
                null, "사과", null, false, ANSWERED_AT);

        assertThat(answer.getTextAnswer()).isEqualTo("사과");
        assertThat(answer.getSelectedOption()).isNull();
        assertThat(answer.getCorrect()).isFalse();
    }

    @Test
    void 음성_답변을_기록할_수_있다() {
        TrainingAnswer answer = TrainingAnswer.record(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 3, QuestionType.LANGUAGE,
                null, null, "media-key-1", null, ANSWERED_AT);

        assertThat(answer.getVoiceMediaKey()).isEqualTo("media-key-1");
        assertThat(answer.getCorrect()).isNull();
    }

    @Test
    void 정답_여부가_null이면_채점_대기_상태로_저장된다() {
        TrainingAnswer answer = TrainingAnswer.record(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 4, QuestionType.LANGUAGE,
                null, null, "media-key-2", null, ANSWERED_AT);

        assertThat(answer.getCorrect()).isNull();
    }

    @Test
    void 문항_유형별로_답변을_기록할_수_있다() {
        for (QuestionType type : QuestionType.values()) {
            TrainingAnswer answer = TrainingAnswer.record(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, type,
                    "1", null, null, true, ANSWERED_AT);

            assertThat(answer.getQuestionType()).isEqualTo(type);
        }
    }
}
