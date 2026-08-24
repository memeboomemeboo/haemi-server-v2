package com.memeboo2.haemi.guardian.memory.presentation.dto;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.memory.application.MemoryWithCreator;
import com.memeboo2.haemi.guardian.memory.domain.Memory;

import java.util.UUID;

public record MemorySummaryResponse(
        UUID id,
        String title,
        String thumbnailKey,
        boolean responded,
        String creatorName,
        GuardianRole creatorRole,
        boolean isMine
) {
    public static MemorySummaryResponse from(MemoryWithCreator mc) {
        Memory m = mc.memory();
        String thumbnail = m.getImages().isEmpty() ? null : m.getImages().get(0).getStorageKey();
        return new MemorySummaryResponse(m.getId(), m.getTitle(), thumbnail, m.isResponded(),
                mc.creatorName(), mc.creatorRole(), mc.isMine());
    }
}
