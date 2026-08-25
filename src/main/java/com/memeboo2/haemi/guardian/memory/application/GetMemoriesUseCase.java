package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
}
