package com.memeboo2.haemi.guardian.memory.presentation.dto;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.memory.application.MemoryWithCreator;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import io.swagger.v3.oas.annotations.media.Schema;

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
        Integer memoryMonth,
        String place,
        List<String> imageKeys,
        boolean responded,
        Instant createdAt,
        @Schema(description = "추억 생성자 이름. 생성자 계정을 특정할 수 없으면(createdBy=null) null")
        String creatorName,
        @Schema(description = "추억 생성자의 어르신 기준 관계 라벨. 생성자가 해당 어르신과의 링크를 해제한 경우 null (A13)")
        GuardianRole creatorRole,
        String creatorRoleLabel,
        boolean isMine
) {
    public static MemoryDetailResponse from(MemoryWithCreator mc) {
        Memory m = mc.memory();
        GuardianRole role = mc.creatorRole();
        return new MemoryDetailResponse(
                m.getId(), m.getElderId(), m.getTitle(), m.getMemo(), m.getMessage(),
                m.getMemoryYear(), m.getMemoryMonth(), m.getPlace(),
                m.getImages().stream().map(img -> img.getStorageKey()).toList(),
                m.isResponded(),
                m.getCreatedAt(),
                mc.creatorName(), role, role == null ? null : role.getLabel(), mc.isMine()
        );
    }
}
