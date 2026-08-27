package com.memeboo2.haemi.elder.home.presentation;

import com.memeboo2.haemi.elder.home.application.GetElderHomeUseCase.ElderHomeData;
import com.memeboo2.haemi.elder.home.presentation.dto.ElderHomeResponse;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ElderHomeResponseTest {

    @Test
    @DisplayName("ElderHomeData로부터 인사말 요약, 최근 추억, 훈련 정보를 매핑한다")
    void from_전체_필드를_매핑한다() {
        UUID memoryId = UUID.randomUUID();
        MemoryItem memory = new MemoryItem(
                memoryId, "제목", "메모", "메시지", 2020, List.of("first", "second"),
                false, Instant.now(), "홍길동", GuardianRole.DAUGHTER);

        ElderHomeData data = new ElderHomeData(3L, 1L, List.of(memory), true, 7);

        ElderHomeResponse response = ElderHomeResponse.from(data);

        assertThat(response.greeting().totalToday()).isEqualTo(3L);
        assertThat(response.greeting().unread()).isEqualTo(1L);

        assertThat(response.recentMemories()).hasSize(1);
        assertThat(response.recentMemories().get(0).id()).isEqualTo(memoryId);
        assertThat(response.recentMemories().get(0).title()).isEqualTo("제목");
        assertThat(response.recentMemories().get(0).firstImageKey()).isEqualTo("first");
        assertThat(response.recentMemories().get(0).responded()).isFalse();

        assertThat(response.training().completedToday()).isTrue();
        assertThat(response.training().streak()).isEqualTo(7);
    }

    @Test
    @DisplayName("이미지가 없는 추억은 firstImageKey가 null이다")
    void from_이미지가_없으면_firstImageKey가_null이다() {
        MemoryItem memory = new MemoryItem(
                UUID.randomUUID(), "제목", null, "메시지", null, List.of(),
                false, Instant.now(), null, null);

        ElderHomeData data = new ElderHomeData(0L, 0L, List.of(memory), false, 0);

        ElderHomeResponse response = ElderHomeResponse.from(data);

        assertThat(response.recentMemories().get(0).firstImageKey()).isNull();
    }
}
