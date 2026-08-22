package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.domain.MemoryRegistered;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterMemoryUseCase {

    private final CareAccessQuery careAccessQuery;
    private final MemoryRepository memoryRepository;
    private final MediaUploadCommand mediaUploadCommand;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UUID execute(UUID guardianId, UUID elderId,
                        String title, String memo, String message, Integer memoryYear,
                        List<UUID> mediaRefIds) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);

        Memory memory = Memory.create(elderId, title, memo, message, memoryYear);

        List<String> storageKeys = mediaRefIds.stream()
                .map(refId -> mediaUploadCommand.confirmUpload(guardianId, refId).toString())
                .toList();
        memory.addImages(storageKeys);

        memory = memoryRepository.save(memory);

        eventPublisher.publishEvent(new MemoryRegistered(memory.getId(), elderId, guardianId));

        return memory.getId();
    }
}
