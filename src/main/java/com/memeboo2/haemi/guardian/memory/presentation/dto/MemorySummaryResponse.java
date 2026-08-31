package com.memeboo2.haemi.guardian.memory.presentation.dto;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.memory.application.MemoryWithCreator;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record MemorySummaryResponse(
        UUID id,
        @Schema(description = "이 추억의 대상 어르신 id. '전체' 탭에서 어르신별 분류·필터에 사용")
        UUID elderId,
        String title,
        String thumbnailKey,
        boolean responded,
        String place,
        Integer memoryYear,
        Integer memoryMonth,
        @Schema(description = "추억 생성자 이름. 생성자 계정을 특정할 수 없으면(createdBy=null) null")
        String creatorName,
        @Schema(description = "추억 생성자의 어르신 기준 관계 라벨. 생성자가 해당 어르신과의 링크를 해제한 경우 null (A13)")
        GuardianRole creatorRole,
        String creatorRoleLabel,
        boolean isMine
) {
    public static MemorySummaryResponse from(MemoryWithCreator mc) {
        return from(mc, null);
    }

    public static MemorySummaryResponse from(MemoryWithCreator mc, MediaUploadCommand mediaUploadCommand) {
        Memory m = mc.memory();
        String thumbnail = m.getImages().isEmpty() ? null : m.getImages().get(0).getStorageKey();
        if (mediaUploadCommand != null) thumbnail = mediaUploadCommand.resolveServingUrl(thumbnail);
        GuardianRole role = mc.creatorRole();
        return new MemorySummaryResponse(m.getId(), m.getElderId(), m.getTitle(), thumbnail, m.isResponded(),
                m.getPlace(), m.getMemoryYear(), m.getMemoryMonth(),
                mc.creatorName(), role, role == null ? null : role.getLabel(), mc.isMine());
    }
}
