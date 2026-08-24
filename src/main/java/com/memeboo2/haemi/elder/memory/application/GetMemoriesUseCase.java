package com.memeboo2.haemi.elder.memory.application;

import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery.MemoryItem;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service("elderGetMemoriesUseCase")
@RequiredArgsConstructor
public class GetMemoriesUseCase {

    private final ElderMemoryQuery elderMemoryQuery;
    private final CareAccessQuery careAccessQuery;

    @ElderAccessChecked
    public List<MemoryItem> execute(UUID elderUserId) {
        UUID elderId = careAccessQuery.elderIdForUser(elderUserId);
        careAccessQuery.requireSelf(elderUserId, elderId);
        return elderMemoryQuery.listForElder(elderId);
    }
}
