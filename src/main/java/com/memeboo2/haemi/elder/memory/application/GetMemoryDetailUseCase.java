package com.memeboo2.haemi.elder.memory.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("elderGetMemoryDetailUseCase")
@RequiredArgsConstructor
public class GetMemoryDetailUseCase {

    private final ElderMemoryQuery elderMemoryQuery;
    private final CareAccessQuery careAccessQuery;

    @ElderAccessChecked
    public MemoryItem execute(UUID elderUserId, UUID memoryId) {
        UUID elderId = careAccessQuery.elderIdForUser(elderUserId);
        return elderMemoryQuery.findForElder(memoryId, elderId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "추억을 찾을 수 없습니다."));
    }
}
