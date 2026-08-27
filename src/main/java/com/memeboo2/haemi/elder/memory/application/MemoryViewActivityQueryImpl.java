package com.memeboo2.haemi.elder.memory.application;

import com.memeboo2.haemi.elder.memory.infrastructure.MemoryViewRepository;
import com.memeboo2.haemi.guardian.api.MemoryViewActivityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemoryViewActivityQueryImpl implements MemoryViewActivityQuery {

    private final MemoryViewRepository memoryViewRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MemoryViewActivity> firstViewedBetween(UUID elderId, Instant from, Instant to) {
        return memoryViewRepository
                .findByElderIdAndFirstViewedAtGreaterThanEqualAndFirstViewedAtLessThan(elderId, from, to)
                .stream()
                .map(view -> new MemoryViewActivity(view.getMemoryId(), view.getFirstViewedAt()))
                .toList();
    }
}
