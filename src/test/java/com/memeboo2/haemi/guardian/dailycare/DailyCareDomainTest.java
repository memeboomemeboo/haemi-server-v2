package com.memeboo2.haemi.guardian.dailycare;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DailyCareDomainTest {

    private final UUID guardianId = UUID.randomUUID();
    private final UUID elderId = UUID.randomUUID();
    private final LocalDate careDate = LocalDate.of(2026, 8, 27);

    @Test
    void text로_생성하면_필드가_올바르게_채워진다() {
        DailyCare dailyCare = DailyCare.text(guardianId, elderId, careDate, "오늘도 좋은 하루 보내세요", 30);

        assertThat(dailyCare.getGuardianId()).isEqualTo(guardianId);
        assertThat(dailyCare.getElderId()).isEqualTo(elderId);
        assertThat(dailyCare.getCareDate()).isEqualTo(careDate);
        assertThat(dailyCare.getText()).isEqualTo("오늘도 좋은 하루 보내세요");
        assertThat(dailyCare.getRetainUntil()).isNotNull();
    }

    @Test
    void text가_공백이면_INVALID_INPUT을_던진다() {
        assertThatThrownBy(() -> DailyCare.text(guardianId, elderId, careDate, "  ", 30))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.memeboo2.haemi.common.error.ErrorCode.INVALID_INPUT);
    }

    @Test
    void text가_100자를_초과하면_INVALID_INPUT을_던진다() {
        String longText = "가".repeat(101);

        assertThatThrownBy(() -> DailyCare.text(guardianId, elderId, careDate, longText, 30))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.memeboo2.haemi.common.error.ErrorCode.INVALID_INPUT);
    }

    @Test
    void voice로_생성하면_필드가_올바르게_채워진다() {
        DailyCare dailyCare = DailyCare.voice(guardianId, elderId, careDate, "media-key", 15, 30);

        assertThat(dailyCare.getMediaKey()).isEqualTo("media-key");
        assertThat(dailyCare.getDurationSeconds()).isEqualTo(15);
        assertThat(dailyCare.getRetainUntil()).isNotNull();
    }

    @Test
    void markViewed는_viewedAt을_한번만_설정한다() {
        DailyCare dailyCare = DailyCare.text(guardianId, elderId, careDate, "안녕하세요", 30);
        Instant first = Instant.parse("2026-08-27T01:00:00Z");
        Instant second = Instant.parse("2026-08-27T02:00:00Z");

        dailyCare.markViewed(first);
        dailyCare.markViewed(second);

        assertThat(dailyCare.getViewedAt()).isEqualTo(first);
    }

    @Test
    void isRead는_viewedAt_존재여부를_반환한다() {
        DailyCare dailyCare = DailyCare.text(guardianId, elderId, careDate, "안녕하세요", 30);

        assertThat(dailyCare.isRead()).isFalse();

        dailyCare.markViewed(Instant.now());

        assertThat(dailyCare.isRead()).isTrue();
    }
}
