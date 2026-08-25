package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.ResponseQuery;
import com.memeboo2.haemi.guardian.memory.infrastructure.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMemoryResponsesUseCase {

    private final MemoryRepository memoryRepository;
    private final CareAccessQuery careAccessQuery;
    private final ResponseQuery responseQuery;

    @Transactional(readOnly = true)
    public List<ResponseQuery.ResponseItem> execute(UUID guardianId, UUID memoryId) {
        var memory = memoryRepository.findById(memoryId)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        careAccessQuery.requireGuardianOf(guardianId, memory.getElderId());
        return responseQuery.findByMemoryId(memoryId);
    }
}
