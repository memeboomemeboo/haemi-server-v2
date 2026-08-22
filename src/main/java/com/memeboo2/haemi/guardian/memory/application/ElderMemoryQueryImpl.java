package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
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

    @Override
    @Transactional(readOnly = true)
    public List<MemoryItem> listForElder(UUID elderId) {
        return memoryRepository.findAllByElderIdWithImages(elderId)
                .stream().map(this::toItem).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemoryItem> findForElder(UUID memoryId, UUID elderId) {
        return memoryRepository.findByIdAndElderIdWithImages(memoryId, elderId)
                .map(this::toItem);
    }

    private MemoryItem toItem(Memory m) {
        return new MemoryItem(
                m.getId(),
                m.getTitle(),
                m.getMemo(),
                m.getMessage(),
                m.getMemoryYear(),
                m.getImages().stream().map(img -> img.getStorageKey()).toList(),
                m.isResponded(),
                m.getCreatedAt()
        );
    }
}
