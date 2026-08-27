package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.guardian.report.domain.WeeklyHighlightOverride;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** WeeklyHighlightOverride의 of 팩토리와 내용 수정을 검증한다. */
class WeeklyHighlightOverrideDomainTest {

    @Test
    void of는_전달받은_값으로_하이라이트_오버라이드를_생성한다() {
        UUID elderId = UUID.randomUUID();
        LocalDate weekStart = LocalDate.of(2026, 8, 24);

        WeeklyHighlightOverride override = WeeklyHighlightOverride.of(elderId, weekStart, "이번 주 하이라이트");

        assertThat(override.getElderId()).isEqualTo(elderId);
        assertThat(override.getWeekStart()).isEqualTo(weekStart);
        assertThat(override.getContent()).isEqualTo("이번 주 하이라이트");
    }

    @Test
    void updateContent는_내용을_교체한다() {
        WeeklyHighlightOverride override = WeeklyHighlightOverride.of(
                UUID.randomUUID(), LocalDate.of(2026, 8, 24), "기존 내용");

        override.updateContent("수정된 내용");

        assertThat(override.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    void updateContent는_여러_줄_내용도_교체할_수_있다() {
        WeeklyHighlightOverride override = WeeklyHighlightOverride.of(
                UUID.randomUUID(), LocalDate.of(2026, 8, 24), "기존 내용");

        override.updateContent("첫째 줄\n둘째 줄");

        assertThat(override.getContent()).isEqualTo("첫째 줄\n둘째 줄");
    }
}
