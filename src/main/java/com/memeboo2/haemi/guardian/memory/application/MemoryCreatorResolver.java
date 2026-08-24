package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** 링크가 해제된 보호자가 만든 추억은 creatorRole=null (A13). */
@Component
@RequiredArgsConstructor
public class MemoryCreatorResolver {

    private final AccountQuery accountQuery;
    private final GuardianElderLinkRepository linkRepository;

    public MemoryWithCreator resolve(Memory memory, UUID viewerGuardianId) {
        UUID createdBy = memory.getCreatedBy();
        String creatorName = null;
        GuardianRole creatorRole = null;
        if (createdBy != null) {
            creatorName = accountQuery.findById(createdBy)
                    .map(AccountQuery.AccountInfo::name).orElse(null);
            creatorRole = linkRepository.findByGuardianIdAndElderId(createdBy, memory.getElderId())
                    .map(link -> link.getRole()).orElse(null);
        }
        boolean isMine = createdBy != null && createdBy.equals(viewerGuardianId);
        return new MemoryWithCreator(memory, creatorName, creatorRole, isMine);
    }
}
