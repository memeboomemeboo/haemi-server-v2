package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ElderMemoryQueryImpl implements ElderMemoryQuery {

    private final MemoryRepository memoryRepository;
    private final AccountQuery accountQuery;
    private final GuardianElderLinkRepository linkRepository;
    private final HaemiClock clock;

    @Override
    @Transactional(readOnly = true)
    public List<MemoryItem> listForElder(UUID elderId) {
        return memoryRepository.findByElderIdSince(elderId, clock.now().minusSeconds(365L * 24 * 3600))
                .stream().map(this::toItem).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemoryItem> findForElder(UUID memoryId, UUID elderId) {
        return memoryRepository.findByIdAndElderIdSinceWithImages(
                        memoryId, elderId, clock.now().minusSeconds(365L * 24 * 3600))
                .map(this::toItem);
    }

    private MemoryItem toItem(Memory m) {
        String creatorName = null;
        GuardianRole creatorRole = null;
        if (m.getCreatedBy() != null) {
            creatorName = accountQuery.findById(m.getCreatedBy()).map(AccountQuery.AccountInfo::name).orElse(null);
            creatorRole = linkRepository.findByGuardianIdAndElderId(m.getCreatedBy(), m.getElderId())
                    .map(link -> link.getRole()).orElse(null);
        }
        return new MemoryItem(
                m.getId(),
                m.getTitle(),
                m.getMemo(),
                m.getMessage(),
                m.getMemoryYear(),
                m.getImages().stream().map(img -> img.getStorageKey()).toList(),
                m.isResponded(),
                m.getCreatedAt(),
                creatorName,
                creatorRole
        );
    }
}
