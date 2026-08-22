package com.memeboo2.haemi.elder.memory.presentation.dto;

import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemorySummary(
        UUID id,
        String title,
        String message,
        Integer memoryYear,
        List<String> imageKeys,
        boolean responded,
        Instant createdAt
) {
    public static MemorySummary from(MemoryItem item) {
        return new MemorySummary(
                item.id(), item.title(), item.message(),
                item.memoryYear(), item.imageKeys(),
                item.responded(), item.createdAt()
        );
    }
}
