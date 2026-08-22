package com.memeboo2.haemi.guardian.memory.presentation.dto;

import com.memeboo2.haemi.guardian.memory.domain.Memory;

import java.util.UUID;

public record MemorySummaryResponse(
        UUID id,
        String title,
        String thumbnailKey,
        boolean responded
) {
    public static MemorySummaryResponse from(Memory m) {
        String thumbnail = m.getImages().isEmpty() ? null : m.getImages().get(0).getStorageKey();
        return new MemorySummaryResponse(m.getId(), m.getTitle(), thumbnail, m.isResponded());
    }
}
