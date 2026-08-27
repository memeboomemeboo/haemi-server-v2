package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.elder.training.domain.DifficultyLevel;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.TrainingDifficulty;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** TrainingDifficulty의 시작과 승급/강등 평가 로직을 검증한다. */
class TrainingDifficultyDomainTest {

    private static final LocalDate DAY1 = LocalDate.of(2026, 8, 20);
    private static final LocalDate DAY2 = DAY1.plusDays(1);
    private static final LocalDate DAY3 = DAY2.plusDays(1);

    @Test
    void start은_LEVEL_1로_난이도를_시작한다() {
        UUID elderId = UUID.randomUUID();

        TrainingDifficulty difficulty = TrainingDifficulty.start(elderId, QuestionType.ORIENTATION);

        assertThat(difficulty.getId()).isNotNull();
        assertThat(difficulty.getElderId()).isEqualTo(elderId);
        assertThat(difficulty.getQuestionType()).isEqualTo(QuestionType.ORIENTATION);
        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_1);
        assertThat(difficulty.getConsecutiveHighDays()).isEqualTo(0);
        assertThat(difficulty.getLastEvaluatedDate()).isNull();
    }

    @Test
    void 정확도가_강등_기준_이하면_레벨이_내려가고_연속일수가_초기화된다() {
        TrainingDifficulty difficulty = TrainingDifficulty.start(UUID.randomUUID(), QuestionType.ORIENTATION);
        difficulty.evaluate(DAY1, 0.9, 0.8, 0.5, 3);
        difficulty.evaluate(DAY2, 0.9, 0.8, 0.5, 3);

        difficulty.evaluate(DAY3, 0.4, 0.8, 0.5, 3);

        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_1);
        assertThat(difficulty.getConsecutiveHighDays()).isEqualTo(0);
        assertThat(difficulty.getLastEvaluatedDate()).isEqualTo(DAY3);
    }

    @Test
    void LEVEL_2에서_강등되면_LEVEL_1로_내려간다() {
        TrainingDifficulty difficulty = TrainingDifficulty.start(UUID.randomUUID(), QuestionType.ORIENTATION);
        difficulty.evaluate(DAY1, 0.9, 0.8, 0.5, 1);
        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_2);

        difficulty.evaluate(DAY2, 0.4, 0.8, 0.5, 1);

        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_1);
    }

    @Test
    void 연속으로_승급_기준을_달성하면_레벨이_올라간다() {
        TrainingDifficulty difficulty = TrainingDifficulty.start(UUID.randomUUID(), QuestionType.ORIENTATION);

        difficulty.evaluate(DAY1, 0.9, 0.8, 0.5, 2);
        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_1);
        assertThat(difficulty.getConsecutiveHighDays()).isEqualTo(1);

        difficulty.evaluate(DAY2, 0.9, 0.8, 0.5, 2);

        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_2);
        assertThat(difficulty.getConsecutiveHighDays()).isEqualTo(0);
    }

    @Test
    void 날짜가_연속되지_않으면_연속일수가_1로_초기화된다() {
        TrainingDifficulty difficulty = TrainingDifficulty.start(UUID.randomUUID(), QuestionType.ORIENTATION);
        difficulty.evaluate(DAY1, 0.9, 0.8, 0.5, 3);
        assertThat(difficulty.getConsecutiveHighDays()).isEqualTo(1);

        LocalDate notConsecutive = DAY1.plusDays(3);
        difficulty.evaluate(notConsecutive, 0.9, 0.8, 0.5, 3);

        assertThat(difficulty.getConsecutiveHighDays()).isEqualTo(1);
        assertThat(difficulty.getLastEvaluatedDate()).isEqualTo(notConsecutive);
    }

    @Test
    void 강등도_승급도_아니면_연속일수만_초기화되고_레벨은_유지된다() {
        TrainingDifficulty difficulty = TrainingDifficulty.start(UUID.randomUUID(), QuestionType.ORIENTATION);
        difficulty.evaluate(DAY1, 0.9, 0.8, 0.5, 5);
        assertThat(difficulty.getConsecutiveHighDays()).isEqualTo(1);

        difficulty.evaluate(DAY2, 0.6, 0.8, 0.5, 5);

        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_1);
        assertThat(difficulty.getConsecutiveHighDays()).isEqualTo(0);
        assertThat(difficulty.getLastEvaluatedDate()).isEqualTo(DAY2);
    }

    @Test
    void LEVEL_3에서는_더이상_승급하지_않는다() {
        TrainingDifficulty difficulty = TrainingDifficulty.start(UUID.randomUUID(), QuestionType.ORIENTATION);
        difficulty.evaluate(DAY1, 0.9, 0.8, 0.5, 1);
        difficulty.evaluate(DAY2, 0.9, 0.8, 0.5, 1);
        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_3);

        difficulty.evaluate(DAY3, 0.9, 0.8, 0.5, 1);

        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_3);
    }

    @Test
    void LEVEL_1에서는_더이상_강등하지_않는다() {
        TrainingDifficulty difficulty = TrainingDifficulty.start(UUID.randomUUID(), QuestionType.ORIENTATION);

        difficulty.evaluate(DAY1, 0.1, 0.8, 0.5, 3);

        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_1);
    }

    @Test
    void DifficultyLevel_속성값이_정확하다() {
        assertThat(DifficultyLevel.LEVEL_1.choiceCount()).isEqualTo(3);
        assertThat(DifficultyLevel.LEVEL_1.yearTolerance()).isEqualTo(10);
        assertThat(DifficultyLevel.LEVEL_1.hintProvided()).isTrue();

        assertThat(DifficultyLevel.LEVEL_3.choiceCount()).isEqualTo(4);
        assertThat(DifficultyLevel.LEVEL_3.hintProvided()).isFalse();
    }
}
