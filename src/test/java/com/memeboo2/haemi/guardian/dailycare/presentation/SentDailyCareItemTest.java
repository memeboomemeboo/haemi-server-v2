package com.memeboo2.haemi.guardian.dailycare.presentation;

import com.memeboo2.haemi.guardian.dailycare.domain.CareType;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.guardian.dailycare.presentation.dto.SentDailyCareItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SentDailyCareItemTest {

    @Test
    @DisplayName("텍스트 케어로부터 응답을 매핑하면 mediaKey는 null이고 read는 false다")
    void from_텍스트_케어를_매핑한다() {
        UUID guardianId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        LocalDate careDate = LocalDate.of(2026, 8, 20);
        DailyCare care = DailyCare.text(guardianId, elderId, careDate, "오늘도 사랑해요", 30);

        SentDailyCareItem item = SentDailyCareItem.from(care);

        assertThat(item.careDate()).isEqualTo(careDate);
        assertThat(item.type()).isEqualTo(CareType.TEXT);
        assertThat(item.text()).isEqualTo("오늘도 사랑해요");
        assertThat(item.mediaKey()).isNull();
        assertThat(item.durationSeconds()).isNull();
        assertThat(item.read()).isFalse();
    }

    @Test
    @DisplayName("음성 케어로부터 응답을 매핑하면 mediaKey와 durationSeconds가 채워진다")
    void from_음성_케어를_매핑한다() {
        UUID guardianId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        LocalDate careDate = LocalDate.of(2026, 8, 21);
        DailyCare care = DailyCare.voice(guardianId, elderId, careDate, "media-key", 45, 30);

        SentDailyCareItem item = SentDailyCareItem.from(care);

        assertThat(item.careDate()).isEqualTo(careDate);
        assertThat(item.type()).isEqualTo(CareType.VOICE);
        assertThat(item.text()).isNull();
        assertThat(item.mediaKey()).isEqualTo("media-key");
        assertThat(item.durationSeconds()).isEqualTo(45);
        assertThat(item.read()).isFalse();
    }

    @Test
    @DisplayName("markViewed 이후 read는 true가 된다")
    void from_열람_이후_read가_true다() {
        DailyCare care = DailyCare.text(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), "메시지", 30);
        care.markViewed(java.time.Instant.now());

        SentDailyCareItem item = SentDailyCareItem.from(care);

        assertThat(item.read()).isTrue();
    }
}
