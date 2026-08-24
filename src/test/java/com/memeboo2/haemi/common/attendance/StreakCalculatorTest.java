package com.memeboo2.haemi.common.attendance;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 25);

    @Test
    void 오늘까지_연속_참여하면_그날수만큼_스트릭이다() {
        Set<LocalDate> dates = Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));

        assertThat(StreakCalculator.currentStreak(dates, TODAY)).isEqualTo(3);
    }

    @Test
    void 오늘_참여가_없어도_어제까지_이어졌으면_스트릭이_유지된다() {
        Set<LocalDate> dates = Set.of(TODAY.minusDays(1), TODAY.minusDays(2));

        assertThat(StreakCalculator.currentStreak(dates, TODAY)).isEqualTo(2);
    }

    @Test
    void 어제도_참여가_없으면_스트릭이_0이다_자정_리셋() {
        Set<LocalDate> dates = Set.of(TODAY.minusDays(2), TODAY.minusDays(3));

        assertThat(StreakCalculator.currentStreak(dates, TODAY)).isEqualTo(0);
    }

    @Test
    void 참여_기록이_없으면_스트릭도_0이다() {
        assertThat(StreakCalculator.currentStreak(Set.of(), TODAY)).isEqualTo(0);
        assertThat(StreakCalculator.bestStreak(Set.of())).isEqualTo(0);
    }

    @Test
    void 최고_기록은_현재가_끊겨도_과거_최장_구간을_반환한다() {
        Set<LocalDate> dates = Set.of(
                TODAY.minusDays(10), TODAY.minusDays(9), TODAY.minusDays(8), TODAY.minusDays(7), TODAY.minusDays(6),
                TODAY.minusDays(1)
        );

        assertThat(StreakCalculator.bestStreak(dates)).isEqualTo(5);
        assertThat(StreakCalculator.currentStreak(dates, TODAY)).isEqualTo(1);
    }
}
