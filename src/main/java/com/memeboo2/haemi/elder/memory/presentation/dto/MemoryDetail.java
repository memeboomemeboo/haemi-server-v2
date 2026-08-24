package com.memeboo2.haemi.elder.memory.presentation.dto;

import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.memeboo2.haemi.guardian.api.GuardianRole;

public record MemoryDetail(
        UUID id,
        String title,
        String memo,
        String message,
        Integer memoryYear,
        List<String> imageKeys,
        boolean responded,
        Instant createdAt,
        String creatorName,
        GuardianRole creatorRole,
        String creatorRoleLabel
) {
    public static MemoryDetail from(MemoryItem item) {
        GuardianRole role = item.creatorRole();
        return new MemoryDetail(
                item.id(), item.title(), item.memo(), item.message(),
                item.memoryYear(), item.imageKeys(),
                item.responded(), item.createdAt(), item.creatorName(), role,
                role == null ? null : role.getLabel()
        );
    }
}
