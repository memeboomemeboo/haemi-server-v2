package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.training.domain.DifficultyLevel;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.TrainingDifficulty;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** KST 날짜 경계와 다음 세션 난이도 규칙을 고정한다. */
class TrainingSessionKstBoundaryPolicyTest {

    @Test
    void 완료_시각으로_계산한_KST_날짜가_자정_경계를_일관되게_가른다() {
        assertThat(HaemiClock.dateInKst(Instant.parse("2026-08-24T14:59:59Z")))
                .isEqualTo(LocalDate.of(2026, 8, 24));
        assertThat(HaemiClock.dateInKst(Instant.parse("2026-08-24T15:00:00Z")))
                .isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    void 이틀_연속_80퍼센트_이상이면_다음_세션_난이도가_한_단계_올라간다() {
        TrainingDifficulty difficulty = TrainingDifficulty.start(UUID.randomUUID(), QuestionType.RECALL);

        difficulty.evaluate(LocalDate.of(2026, 8, 24), 0.8, 0.8, 0.4, 2);
        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_1);
        difficulty.evaluate(LocalDate.of(2026, 8, 25), 0.8, 0.8, 0.4, 2);

        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_2);
    }

    @Test
    void 낮은_정확도이면_즉시_한_단계_낮춘다() {
        TrainingDifficulty difficulty = TrainingDifficulty.start(UUID.randomUUID(), QuestionType.RECALL);
        difficulty.evaluate(LocalDate.of(2026, 8, 24), 0.8, 0.8, 0.4, 1);
        difficulty.evaluate(LocalDate.of(2026, 8, 25), 0.0, 0.8, 0.4, 2);

        assertThat(difficulty.getLevel()).isEqualTo(DifficultyLevel.LEVEL_1);
    }
}
