package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMemoryDetailUseCase {

    private final CareAccessQuery careAccessQuery;
    private final MemoryRepository memoryRepository;

    @Transactional(readOnly = true)
    public Memory execute(UUID guardianId, UUID memoryId) {
        Memory memory = memoryRepository.findByIdWithImages(memoryId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        careAccessQuery.requireGuardianOf(guardianId, memory.getElderId());
        return memory;
    }
}
