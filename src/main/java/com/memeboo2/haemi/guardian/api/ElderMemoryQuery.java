package com.memeboo2.haemi.guardian.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 어르신이 자신에게 등록된 추억을 조회하기 위한 계약.
 * 소유: guardian/memory. elder/memory 가 호출.
 */
public interface ElderMemoryQuery {

    List<MemoryItem> listForElder(UUID elderId);

    Optional<MemoryItem> findForElder(UUID memoryId, UUID elderId);

    record MemoryItem(
            UUID id,
            String title,
            String memo,
            String message,
            Integer memoryYear,
            List<String> imageKeys,
            boolean responded,
            Instant createdAt
    ) {}
}
