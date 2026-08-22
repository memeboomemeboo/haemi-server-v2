package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.MemoryQuery;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** MemoryQueryStub을 대체하는 실구현. bean name이 "memoryQueryImpl"이어야 한다. */
@Service("memoryQueryImpl")
@RequiredArgsConstructor
public class MemoryQueryImpl implements MemoryQuery {

    private final MemoryRepository memoryRepository;
    private final HaemiClock clock;

    @Override
    @Transactional(readOnly = true)
    public List<MemoryMaterial> materialsFor(UUID elderId, int limit) {
        Instant oneYearAgo = clock.now().minusSeconds(365L * 24 * 3600);
        return memoryRepository.findByElderIdSince(elderId, oneYearAgo).stream()
                .filter(m -> !m.getImages().isEmpty())
                .limit(limit)
                .map(m -> new MemoryMaterial(
                        m.getId(),
                        m.getTitle(),
                        m.getMemoryYear(),
                        m.getImages().stream().map(img -> img.getStorageKey()).toList(),
                        clock.today()
                ))
                .toList();
    }
}
