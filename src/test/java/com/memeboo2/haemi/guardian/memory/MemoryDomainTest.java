package com.memeboo2.haemi.guardian.memory;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoryDomainTest {

    private final UUID elderId = UUID.randomUUID();

    @Test
    void create는_필드를_올바르게_채운다() {
        Memory memory = Memory.create(elderId, "가족 여행", "즐거웠던 기억", "오늘도 즐거운 하루 보내세요", 2020);

        assertThat(memory.getElderId()).isEqualTo(elderId);
        assertThat(memory.getTitle()).isEqualTo("가족 여행");
        assertThat(memory.getMemo()).isEqualTo("즐거웠던 기억");
        assertThat(memory.getMessage()).isEqualTo("오늘도 즐거운 하루 보내세요");
        assertThat(memory.getMemoryYear()).isEqualTo(2020);
        assertThat(memory.isResponded()).isFalse();
        assertThat(memory.getImages()).isEmpty();
    }

    @Test
    void memo가_300자를_초과하면_INVALID_INPUT을_던진다() {
        String longMemo = "가".repeat(301);

        assertThatThrownBy(() -> Memory.create(elderId, "제목", longMemo, "메시지", 2020))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void addImages는_최대_4장을_초과하면_INVALID_INPUT을_던진다() {
        Memory memory = Memory.create(elderId, "제목", "메모", "메시지", 2020);
        memory.addImages(List.of("k1", "k2", "k3", "k4"));

        assertThatThrownBy(() -> memory.addImages(List.of("k5")))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void addImages는_4장까지_정상적으로_추가된다() {
        Memory memory = Memory.create(elderId, "제목", "메모", "메시지", 2020);

        memory.addImages(List.of("k1", "k2", "k3", "k4"));

        assertThat(memory.getImages()).hasSize(4);
    }

    @Test
    void update는_이미지를_교체한다() {
        Memory memory = Memory.create(elderId, "제목", "메모", "메시지", 2020);
        memory.addImages(List.of("k1", "k2"));

        memory.update("새 제목", "새 메모", "새 메시지", 2021, List.of("k3"));

        assertThat(memory.getTitle()).isEqualTo("새 제목");
        assertThat(memory.getMemo()).isEqualTo("새 메모");
        assertThat(memory.getMessage()).isEqualTo("새 메시지");
        assertThat(memory.getMemoryYear()).isEqualTo(2021);
        assertThat(memory.getImages()).hasSize(1);
        assertThat(memory.getImages().get(0).getStorageKey()).isEqualTo("k3");
    }

    @Test
    void markResponded는_responded_플래그를_true로_설정한다() {
        Memory memory = Memory.create(elderId, "제목", "메모", "메시지", 2020);

        memory.markResponded();

        assertThat(memory.isResponded()).isTrue();
    }

    @Test
    void delete는_소프트_삭제_처리한다() {
        Memory memory = Memory.create(elderId, "제목", "메모", "메시지", 2020);
        Instant now = Instant.parse("2026-08-27T00:00:00Z");

        memory.delete(now);

        assertThat(memory.isDeleted()).isTrue();
    }
}
