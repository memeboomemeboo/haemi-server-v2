package com.memeboo2.haemi.guardian.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CIST-TRN-003/004 재료. 어르신에게 전달된 추억 중 이미지가 있는 것.
 * 소유: 황정빈. 1단계 초반에 스텁(빈 리스트 반환) 머지.
 */
public interface MemoryQuery {

    List<MemoryMaterial> materialsFor(UUID elderId, int limit);

    record MemoryMaterial(
        UUID id,
        String title,
        Integer memoryYear,
        List<String> imageKeys,
        LocalDate registeredAt
    ) {}
}
