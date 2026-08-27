package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service("guardianGetMemoriesUseCase")
@RequiredArgsConstructor
public class GetMemoriesUseCase {

    private final CareAccessQuery careAccessQuery;
    private final MemoryRepository memoryRepository;
    private final MemoryCreatorResolver creatorResolver;
    private final HaemiClock clock;

    @Transactional(readOnly = true)
    public List<MemoryWithCreator> execute(UUID guardianId, UUID elderId) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);

        Instant oneYearAgo = clock.now().minusSeconds(365L * 24 * 3600);
        List<Memory> memories = memoryRepository.findByElderIdSince(elderId, oneYearAgo);
        return creatorResolver.resolveAll(memories, elderId, guardianId);
    }

    /**
     * 추억 앨범 "전체" 탭(#100 M4): elderId 미지정 시 접근 가능한 전 어르신의 추억을 통합 조회한다.
     * 어르신마다 (보호자,어르신) 역할이 다를 수 있어, 어르신별로 역할 링크를 정확히 해석해 합친 뒤
     * 최신순으로 정렬한다.
     */
    @Transactional(readOnly = true)
    public List<MemoryWithCreator> executeAll(UUID guardianId) {
        Instant oneYearAgo = clock.now().minusSeconds(365L * 24 * 3600);
        return careAccessQuery.accessibleElders(guardianId).stream()
                .flatMap(elderId -> creatorResolver.resolveAll(
                        memoryRepository.findByElderIdSince(elderId, oneYearAgo), elderId, guardianId).stream())
                .sorted(Comparator.comparing((MemoryWithCreator mc) -> mc.memory().getCreatedAt()).reversed())
                .toList();
    }
}
