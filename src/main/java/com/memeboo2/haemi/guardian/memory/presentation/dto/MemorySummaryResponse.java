package com.memeboo2.haemi.guardian.memory.presentation.dto;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.memory.application.MemoryWithCreator;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record MemorySummaryResponse(
        UUID id,
        String title,
        String thumbnailKey,
        boolean responded,
        @Schema(description = "추억 생성자 이름. 생성자 계정을 특정할 수 없으면(createdBy=null) null")
        String creatorName,
        @Schema(description = "추억 생성자의 어르신 기준 관계 라벨. 생성자가 해당 어르신과의 링크를 해제한 경우 null (A13)")
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
