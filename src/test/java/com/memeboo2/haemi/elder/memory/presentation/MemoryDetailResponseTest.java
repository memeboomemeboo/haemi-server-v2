package com.memeboo2.haemi.elder.memory.presentation;

import com.memeboo2.haemi.elder.memory.presentation.dto.MemoryDetail;
import com.memeboo2.haemi.elder.memory.presentation.dto.MemorySummary;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemoryDetailResponseTest {

    @Test
    @DisplayName("MemoryDetail.from은 MemoryItem의 전체 필드와 관계 라벨을 매핑한다")
    void memoryDetail_from_전체_필드를_매핑한다() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-01T00:00:00Z");
        MemoryItem item = new MemoryItem(
                id, "제목", "메모", "메시지", 2020, List.of("img1", "img2"),
                true, createdAt, "홍길동", GuardianRole.SON);

        MemoryDetail detail = MemoryDetail.from(item);

        assertThat(detail.id()).isEqualTo(id);
        assertThat(detail.title()).isEqualTo("제목");
        assertThat(detail.memo()).isEqualTo("메모");
        assertThat(detail.message()).isEqualTo("메시지");
        assertThat(detail.memoryYear()).isEqualTo(2020);
        assertThat(detail.imageKeys()).containsExactly("img1", "img2");
        assertThat(detail.responded()).isTrue();
        assertThat(detail.createdAt()).isEqualTo(createdAt);
        assertThat(detail.creatorName()).isEqualTo("홍길동");
        assertThat(detail.creatorRole()).isEqualTo(GuardianRole.SON);
        assertThat(detail.creatorRoleLabel()).isEqualTo("아들");
    }

    @Test
    @DisplayName("MemoryDetail.from은 creatorRole이 null이면 creatorRoleLabel도 null이다")
    void memoryDetail_from_creatorRole_null이면_roleLabel도_null이다() {
        MemoryItem item = new MemoryItem(
                UUID.randomUUID(), "제목", null, "메시지", null, List.of(),
                false, Instant.now(), null, null);

        MemoryDetail detail = MemoryDetail.from(item);

        assertThat(detail.creatorName()).isNull();
        assertThat(detail.creatorRole()).isNull();
        assertThat(detail.creatorRoleLabel()).isNull();
    }

    @Test
    @DisplayName("MemorySummary.from은 memo를 제외한 MemoryItem 필드를 매핑한다")
    void memorySummary_from_전체_필드를_매핑한다() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-01T00:00:00Z");
        MemoryItem item = new MemoryItem(
                id, "제목", "메모", "메시지", 2020, List.of("img1"),
                true, createdAt, "김철수", GuardianRole.GRANDSON);

        MemorySummary summary = MemorySummary.from(item);

        assertThat(summary.id()).isEqualTo(id);
        assertThat(summary.title()).isEqualTo("제목");
        assertThat(summary.message()).isEqualTo("메시지");
        assertThat(summary.memoryYear()).isEqualTo(2020);
        assertThat(summary.imageKeys()).containsExactly("img1");
        assertThat(summary.responded()).isTrue();
        assertThat(summary.createdAt()).isEqualTo(createdAt);
        assertThat(summary.creatorName()).isEqualTo("김철수");
        assertThat(summary.creatorRole()).isEqualTo(GuardianRole.GRANDSON);
        assertThat(summary.creatorRoleLabel()).isEqualTo("손자");
    }

    @Test
    @DisplayName("어르신 추억 DTO는 영구 storage key를 serving URL로 변환한다")
    void memoryDtos_이미지_storage_key를_serving_URL로_변환한다() {
        MemoryItem item = new MemoryItem(
                UUID.randomUUID(), "제목", "메모", "메시지", 2020, List.of("memory_image/photo.jpg"),
                false, Instant.now(), "홍길동", GuardianRole.SON);
        MediaUploadCommand media = mock(MediaUploadCommand.class);
        when(media.resolveServingUrl("memory_image/photo.jpg")).thenReturn("https://cdn.example/photo.jpg");

        assertThat(MemorySummary.from(item, media).imageKeys()).containsExactly("https://cdn.example/photo.jpg");
        assertThat(MemoryDetail.from(item, media).imageKeys()).containsExactly("https://cdn.example/photo.jpg");
    }

    @Test
    @DisplayName("MemorySummary.from은 creatorRole이 null이면 creatorRoleLabel도 null이다")
    void memorySummary_from_creatorRole_null이면_roleLabel도_null이다() {
        MemoryItem item = new MemoryItem(
                UUID.randomUUID(), "제목", "메모", "메시지", null, List.of(),
                false, Instant.now(), null, null);

        MemorySummary summary = MemorySummary.from(item);

        assertThat(summary.creatorRole()).isNull();
        assertThat(summary.creatorRoleLabel()).isNull();
    }
}
