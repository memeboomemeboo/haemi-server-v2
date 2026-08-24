package com.memeboo2.haemi.elder.memory.presentation.dto;

import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(description = "추억 생성자 이름. 생성자 계정을 특정할 수 없으면(createdBy=null) null")
        String creatorName,
        @Schema(description = "추억 생성자의 어르신 기준 관계 라벨. 생성자가 해당 어르신과의 링크를 해제한 경우 null (A13)")
        GuardianRole creatorRole
) {
    public static MemoryDetail from(MemoryItem item) {
        return new MemoryDetail(
                item.id(), item.title(), item.memo(), item.message(),
                item.memoryYear(), item.imageKeys(),
                item.responded(), item.createdAt(), item.creatorName(), item.creatorRole()
        );
    }
}
