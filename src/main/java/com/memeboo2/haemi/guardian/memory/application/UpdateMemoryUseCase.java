package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateMemoryUseCase {

    private final MemoryRepository memoryRepository;
    private final MediaUploadCommand mediaUploadCommand;

    @Transactional
    public void execute(UUID guardianId, UUID memoryId,
                        String title, String memo, String message, Integer memoryYear,
                        List<UUID> mediaRefIds) {
        Memory memory = memoryRepository.findByIdWithImages(memoryId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));

        // R5: 생성자 본인만 수정 가능
        if (!guardianId.equals(memory.getCreatedBy())) {
            throw new DomainException(ErrorCode.NOT_RESOURCE_OWNER);
        }

        List<String> storageKeys = mediaRefIds.stream()
                .map(refId -> mediaUploadCommand.confirmUpload(guardianId, refId).toString())
                .toList();

        memory.update(title, memo, message, memoryYear, storageKeys);
    }
}
