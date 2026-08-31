package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import com.memeboo2.haemi.platform.api.MediaPurpose;
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

    /** 장소·월이 없던 기존 호출부 호환용. */
    @Transactional
    public void execute(UUID guardianId, UUID memoryId,
                        String title, String memo, String message, Integer memoryYear,
                        List<UUID> mediaRefIds) {
        execute(guardianId, memoryId, title, memo, message, memoryYear, null, null, mediaRefIds);
    }

    @Transactional
    public void execute(UUID guardianId, UUID memoryId,
                        String title, String memo, String message, Integer memoryYear,
                        Integer memoryMonth, String place,
                        List<UUID> mediaRefIds) {
        Memory memory = memoryRepository.findByIdWithImages(memoryId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));

        // R5: 생성자 본인만 수정 가능
        if (!guardianId.equals(memory.getCreatedBy())) {
            throw new DomainException(ErrorCode.NOT_RESOURCE_OWNER);
        }

        if (mediaRefIds == null) {
            mediaRefIds = List.of();
        }
        int maxCount = mediaUploadCommand.memoryImageMaxCount();
        if (mediaRefIds.size() > maxCount) {
            throw new DomainException(ErrorCode.INVALID_INPUT,
                    "추억 이미지는 최대 " + maxCount + "장까지 등록할 수 있습니다.");
        }

        List<String> storageKeys = mediaRefIds.stream()
                .map(refId -> mediaUploadCommand.confirmUploadKey(guardianId, refId, MediaPurpose.MEMORY_IMAGE))
                .toList();

        memory.update(title, memo, message, memoryYear, memoryMonth, place, storageKeys);
    }
}
