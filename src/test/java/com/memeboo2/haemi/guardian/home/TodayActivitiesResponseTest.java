package com.memeboo2.haemi.guardian.home;

import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase.ActivityEntry;
import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase.ActivityKind;
import com.memeboo2.haemi.guardian.home.presentation.dto.TodayActivitiesResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TodayActivitiesResponseTest {

    @Test
    void from_mapsEntriesToItems() {
        Instant now = Instant.now();
        UUID memoryId = UUID.randomUUID();
        List<ActivityEntry> entries = List.of(
                new ActivityEntry(now, ActivityKind.COGNITIVE_TRAINING, "인지 활동 완료", null),
                new ActivityEntry(now.plusSeconds(60), ActivityKind.MEMORY_RESPONSE, "음성 메시지 도착", memoryId),
                new ActivityEntry(now.plusSeconds(120), ActivityKind.DAILY_CARE_READ, "하루 한마디 열람", null)
        );

        TodayActivitiesResponse response = TodayActivitiesResponse.from(entries);

        assertThat(response.items()).hasSize(3);

        TodayActivitiesResponse.Item first = response.items().get(0);
        assertThat(first.at()).isEqualTo(now);
        assertThat(first.kind()).isEqualTo(ActivityKind.COGNITIVE_TRAINING);
        assertThat(first.summary()).isEqualTo("인지 활동 완료");
        assertThat(first.memoryId()).isNull();

        TodayActivitiesResponse.Item second = response.items().get(1);
        assertThat(second.kind()).isEqualTo(ActivityKind.MEMORY_RESPONSE);
        assertThat(second.memoryId()).isEqualTo(memoryId);
    }

    @Test
    void from_emptyList_returnsEmptyItems() {
        TodayActivitiesResponse response = TodayActivitiesResponse.from(List.of());

        assertThat(response.items()).isEmpty();
    }
}
