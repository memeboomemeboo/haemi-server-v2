package com.memeboo2.haemi.guardian.memory.presentation.dto;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.memory.application.MemoryWithCreator;
import com.memeboo2.haemi.guardian.memory.domain.Memory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemoryDetailResponse(
        UUID id,
        UUID elderId,
        String title,
        String memo,
        String message,
        Integer memoryYear,
        List<String> imageKeys,
        boolean responded,
        Instant createdAt,
        String creatorName,
        GuardianRole creatorRole,
        boolean isMine
) {
    public static MemoryDetailResponse from(MemoryWithCreator mc) {
        Memory m = mc.memory();
        return new MemoryDetailResponse(
                m.getId(), m.getElderId(), m.getTitle(), m.getMemo(), m.getMessage(),
                m.getMemoryYear(),
                m.getImages().stream().map(img -> img.getStorageKey()).toList(),
                m.isResponded(),
                m.getCreatedAt(),
                mc.creatorName(), mc.creatorRole(), mc.isMine()
        );
    }
}
