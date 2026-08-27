package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** TrainingSession의 팩토리와 상태 전이를 검증한다. */
class TrainingSessionDomainTest {

    private static final Instant NOW = Instant.parse("2026-08-27T00:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 27);

    @Test
    void start은_진행중_상태로_첫_문항부터_세션을_시작한다() {
        UUID elderId = UUID.randomUUID();

        TrainingSession session = TrainingSession.start(elderId, NOW, TODAY);

        assertThat(session.getId()).isNotNull();
        assertThat(session.getElderId()).isEqualTo(elderId);
        assertThat(session.getActiveElderId()).isEqualTo(elderId);
        assertThat(session.getSessionDate()).isEqualTo(TODAY);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.getCurrentStep()).isEqualTo(QuestionType.ORIENTATION);
        assertThat(session.getCurrentQuestionNumber()).isEqualTo(1);
        assertThat(session.getStartedAt()).isEqualTo(NOW);
        assertThat(session.getCompletedAt()).isNull();
    }

    @Test
    void start은_호출할_때마다_새로운_id를_부여한다() {
        UUID elderId = UUID.randomUUID();

        TrainingSession first = TrainingSession.start(elderId, NOW, TODAY);
        TrainingSession second = TrainingSession.start(elderId, NOW, TODAY);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void 마지막이_아닌_문항을_완료하면_다음_단계로만_이동한다() {
        TrainingSession session = TrainingSession.start(UUID.randomUUID(), NOW, TODAY);

        session.completeCurrentQuestion(
                session.getId(), QuestionType.ORIENTATION, 1, QuestionType.ORIENTATION, false, NOW);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.getCurrentStep()).isEqualTo(QuestionType.ORIENTATION);
        assertThat(session.getCurrentQuestionNumber()).isEqualTo(2);
        assertThat(session.getCompletedAt()).isNull();
        assertThat(session.getActiveElderId()).isNotNull();
    }

    @Test
    void 마지막_문항을_완료하면_세션이_완료되고_진행_상태가_초기화된다() {
        TrainingSession session = TrainingSession.start(UUID.randomUUID(), NOW, TODAY);
        Instant completedAt = Instant.parse("2026-08-27T00:10:00Z");

        session.completeCurrentQuestion(
                session.getId(), QuestionType.ORIENTATION, 1, null, true, completedAt);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.getCurrentStep()).isNull();
        assertThat(session.getCurrentQuestionNumber()).isNull();
        assertThat(session.getCompletedAt()).isEqualTo(completedAt);
        assertThat(session.getActiveElderId()).isNull();
    }

    @Test
    void 이미_완료한_세션을_다시_완료하려_하면_예외가_발생한다() {
        TrainingSession session = TrainingSession.start(UUID.randomUUID(), NOW, TODAY);
        session.completeCurrentQuestion(session.getId(), QuestionType.ORIENTATION, 1, null, true, NOW);

        assertThatThrownBy(() -> session.completeCurrentQuestion(
                session.getId(), QuestionType.ORIENTATION, 1, null, true, NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void 다른_세션_id로_완료를_시도하면_예외가_발생한다() {
        TrainingSession session = TrainingSession.start(UUID.randomUUID(), NOW, TODAY);

        assertThatThrownBy(() -> session.completeCurrentQuestion(
                UUID.randomUUID(), QuestionType.ORIENTATION, 1, QuestionType.ORIENTATION, false, NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void 현재_단계와_다른_단계로_완료를_시도하면_예외가_발생한다() {
        TrainingSession session = TrainingSession.start(UUID.randomUUID(), NOW, TODAY);

        assertThatThrownBy(() -> session.completeCurrentQuestion(
                session.getId(), QuestionType.RECALL, 1, QuestionType.RECALL, false, NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void 현재_문항_번호와_다른_번호로_완료를_시도하면_예외가_발생한다() {
        TrainingSession session = TrainingSession.start(UUID.randomUUID(), NOW, TODAY);

        assertThatThrownBy(() -> session.completeCurrentQuestion(
                session.getId(), QuestionType.ORIENTATION, 2, QuestionType.ORIENTATION, false, NOW))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void 단계가_바뀌는_문항_완료시_다음_단계로_정확히_전환된다() {
        TrainingSession session = TrainingSession.start(UUID.randomUUID(), NOW, TODAY);

        session.completeCurrentQuestion(
                session.getId(), QuestionType.ORIENTATION, 1, QuestionType.RECALL, false, NOW);

        assertThat(session.getCurrentStep()).isEqualTo(QuestionType.RECALL);
        assertThat(session.getCurrentQuestionNumber()).isEqualTo(2);
    }

    @Test
    void 여러_날짜에_걸쳐도_동일한_세션이_이어질_수_있다() {
        LocalDate startDate = LocalDate.of(2026, 8, 20);
        TrainingSession session = TrainingSession.start(UUID.randomUUID(), NOW, startDate);

        session.completeCurrentQuestion(
                session.getId(), QuestionType.ORIENTATION, 1, QuestionType.ORIENTATION, false,
                Instant.parse("2026-08-27T00:00:00Z"));

        assertThat(session.getSessionDate()).isEqualTo(startDate);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
    }
}
