package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.elder.training.domain.DifficultyLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DifficultyLevelTest {

    @Test
    void LEVEL_1의_속성을_확인한다() {
        assertThat(DifficultyLevel.LEVEL_1.choiceCount()).isEqualTo(3);
        assertThat(DifficultyLevel.LEVEL_1.yearTolerance()).isEqualTo(10);
        assertThat(DifficultyLevel.LEVEL_1.hintProvided()).isTrue();
    }

    @Test
    void LEVEL_2의_속성을_확인한다() {
        assertThat(DifficultyLevel.LEVEL_2.choiceCount()).isEqualTo(4);
        assertThat(DifficultyLevel.LEVEL_2.yearTolerance()).isEqualTo(5);
        assertThat(DifficultyLevel.LEVEL_2.hintProvided()).isFalse();
    }

    @Test
    void LEVEL_3의_속성을_확인한다() {
        assertThat(DifficultyLevel.LEVEL_3.choiceCount()).isEqualTo(4);
        assertThat(DifficultyLevel.LEVEL_3.yearTolerance()).isEqualTo(3);
        assertThat(DifficultyLevel.LEVEL_3.hintProvided()).isFalse();
    }

    @Test
    void raise는_한_단계_올린다() {
        assertThat(DifficultyLevel.LEVEL_1.raise()).isEqualTo(DifficultyLevel.LEVEL_2);
        assertThat(DifficultyLevel.LEVEL_2.raise()).isEqualTo(DifficultyLevel.LEVEL_3);
    }

    @Test
    void raise는_최대_레벨에서_유지된다() {
        assertThat(DifficultyLevel.LEVEL_3.raise()).isEqualTo(DifficultyLevel.LEVEL_3);
    }

    @Test
    void lower는_한_단계_내린다() {
        assertThat(DifficultyLevel.LEVEL_3.lower()).isEqualTo(DifficultyLevel.LEVEL_2);
        assertThat(DifficultyLevel.LEVEL_2.lower()).isEqualTo(DifficultyLevel.LEVEL_1);
    }

    @Test
    void lower는_최소_레벨에서_유지된다() {
        assertThat(DifficultyLevel.LEVEL_1.lower()).isEqualTo(DifficultyLevel.LEVEL_1);
    }

    @Test
    void raise_후_lower하면_원래_레벨로_돌아온다() {
        assertThat(DifficultyLevel.LEVEL_1.raise().lower()).isEqualTo(DifficultyLevel.LEVEL_1);
        assertThat(DifficultyLevel.LEVEL_2.raise().lower()).isEqualTo(DifficultyLevel.LEVEL_2);
    }
}
