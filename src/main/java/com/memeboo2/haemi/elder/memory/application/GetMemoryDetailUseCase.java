package com.memeboo2.haemi.elder.memory.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMemoryDetailUseCase {

    private final ElderMemoryQuery elderMemoryQuery;

    public MemoryItem execute(UUID elderId, UUID memoryId) {
        return elderMemoryQuery.findForElder(memoryId, elderId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "추억을 찾을 수 없습니다."));
    }
}
