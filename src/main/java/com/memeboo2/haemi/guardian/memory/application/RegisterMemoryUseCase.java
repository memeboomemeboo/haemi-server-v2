package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.domain.MemoryRegistered;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import com.memeboo2.haemi.platform.api.MediaPurpose;
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

    /** 장소·월이 없던 기존 호출부 호환용. */
    @Transactional
    public UUID execute(UUID guardianId, UUID elderId,
                        String title, String memo, String message, Integer memoryYear,
                        List<UUID> mediaRefIds) {
        return execute(guardianId, elderId, title, memo, message, memoryYear, null, null, mediaRefIds);
    }

    @Transactional
    public UUID execute(UUID guardianId, UUID elderId,
                        String title, String memo, String message, Integer memoryYear,
                        Integer memoryMonth, String place,
                        List<UUID> mediaRefIds) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);

        if (mediaRefIds == null) {
            mediaRefIds = List.of();
        }
        int maxCount = mediaUploadCommand.memoryImageMaxCount();
        if (mediaRefIds.size() > maxCount) {
            throw new DomainException(ErrorCode.INVALID_INPUT,
                    "추억 이미지는 최대 " + maxCount + "장까지 등록할 수 있습니다.");
        }

        Memory memory = Memory.create(elderId, title, memo, message, memoryYear, memoryMonth, place);

        List<String> storageKeys = mediaRefIds.stream()
                .map(refId -> mediaUploadCommand.confirmUpload(guardianId, refId, MediaPurpose.MEMORY_IMAGE).toString())
                .toList();
        memory.addImages(storageKeys);

        memory = memoryRepository.save(memory);

        eventPublisher.publishEvent(new MemoryRegistered(memory.getId(), elderId, guardianId));

        return memory.getId();
    }
}
