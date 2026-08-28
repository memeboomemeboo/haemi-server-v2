package com.memeboo2.haemi.guardian.home;

import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase.ActivityEntry;
import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase.ActivityType;
import com.memeboo2.haemi.guardian.home.presentation.dto.TodayActivitiesResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TodayActivitiesResponseTest {

    @Test
    void from_mapsEntriesToItems() {
        Instant now = Instant.now();
        UUID memoryId = UUID.randomUUID();
        List<ActivityEntry> entries = List.of(
                new ActivityEntry(now, ActivityType.TRAINING_COMPLETED, "인지 활동 완료",
                        Map.of("accuracy", 80)),
                new ActivityEntry(now.plusSeconds(60), ActivityType.RESPONSE_SENT, "추억 답변 완료",
                        Map.of("memoryId", memoryId, "responseType", "VOICE")),
                new ActivityEntry(now.plusSeconds(120), ActivityType.GREETING_READ, "하루 한마디 열람", Map.of())
        );

        LocalDate date = LocalDate.of(2026, 8, 27);
        TodayActivitiesResponse response = TodayActivitiesResponse.from(date, entries);

        assertThat(response.date()).isEqualTo(date);
        assertThat(response.items()).hasSize(3);

        TodayActivitiesResponse.Item first = response.items().get(0);
        assertThat(first.occurredAt()).isEqualTo(now);
        assertThat(first.type()).isEqualTo(ActivityType.TRAINING_COMPLETED);
        assertThat(first.title()).isEqualTo("인지 활동 완료");
        assertThat(first.detail()).containsEntry("accuracy", 80);

        TodayActivitiesResponse.Item second = response.items().get(1);
        assertThat(second.type()).isEqualTo(ActivityType.RESPONSE_SENT);
        assertThat(second.detail()).containsEntry("memoryId", memoryId);
    }

    @Test
    void from_emptyList_returnsEmptyItems() {
        TodayActivitiesResponse response = TodayActivitiesResponse.from(LocalDate.of(2026, 8, 27), List.of());

        assertThat(response.items()).isEmpty();
    }
}
