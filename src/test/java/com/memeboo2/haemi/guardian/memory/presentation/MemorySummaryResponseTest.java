package com.memeboo2.haemi.guardian.memory.presentation;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.memory.application.MemoryWithCreator;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.presentation.dto.MemorySummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MemorySummaryResponseTest {

    @Test
    @DisplayName("MemoryWithCreator로부터 썸네일, 생성자, 내 것 여부를 매핑한다")
    void from_전체_필드를_매핑한다() {
        UUID elderId = UUID.randomUUID();
        Memory memory = Memory.create(elderId, "제목", "메모", "메시지", 2020, 4, "구지면");
        memory.addImages(java.util.List.of("thumb1", "thumb2"));

        MemoryWithCreator withCreator = new MemoryWithCreator(memory, "이보호자", GuardianRole.DAUGHTER, true);

        MemorySummaryResponse response = MemorySummaryResponse.from(withCreator);

        assertThat(response.id()).isEqualTo(memory.getId());
        assertThat(response.elderId()).isEqualTo(elderId);
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.thumbnailKey()).isEqualTo("thumb1");
        assertThat(response.responded()).isFalse();
        assertThat(response.place()).isEqualTo("구지면");
        assertThat(response.memoryYear()).isEqualTo(2020);
        assertThat(response.memoryMonth()).isEqualTo(4);
        assertThat(response.creatorName()).isEqualTo("이보호자");
        assertThat(response.creatorRole()).isEqualTo(GuardianRole.DAUGHTER);
        assertThat(response.creatorRoleLabel()).isEqualTo("딸");
        assertThat(response.isMine()).isTrue();
    }

    @Test
    @DisplayName("이미지가 없으면 thumbnailKey가 null이고, creatorRole이 없으면 roleLabel도 null이다")
    void from_이미지_없고_creatorRole_없으면_null이다() {
        Memory memory = Memory.create(UUID.randomUUID(), "제목", null, "메시지", null);

        MemoryWithCreator withCreator = new MemoryWithCreator(memory, null, null, false);

        MemorySummaryResponse response = MemorySummaryResponse.from(withCreator);

        assertThat(response.thumbnailKey()).isNull();
        assertThat(response.creatorName()).isNull();
        assertThat(response.creatorRole()).isNull();
        assertThat(response.creatorRoleLabel()).isNull();
        assertThat(response.isMine()).isFalse();
    }
}
