package com.memeboo2.haemi.guardian.memory.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.memory.domain.Memory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /** 목록 조회용. 생성자 계정·역할 링크를 일괄 조회해 N+1을 피한다. */
    public List<MemoryWithCreator> resolveAll(List<Memory> memories, UUID elderId, UUID viewerGuardianId) {
        List<UUID> creatorIds = memories.stream()
                .map(Memory::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, String> namesByCreator = accountQuery.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(AccountQuery.AccountInfo::userId, AccountQuery.AccountInfo::name));
        Map<UUID, GuardianRole> rolesByCreator = linkRepository.findAllByGuardianIdInAndElderId(creatorIds, elderId).stream()
                .collect(Collectors.toMap(GuardianElderLink::getGuardianId, GuardianElderLink::getRole));

        return memories.stream()
                .map(memory -> {
                    UUID createdBy = memory.getCreatedBy();
                    String creatorName = createdBy == null ? null : namesByCreator.get(createdBy);
                    GuardianRole creatorRole = createdBy == null ? null : rolesByCreator.get(createdBy);
                    boolean isMine = createdBy != null && createdBy.equals(viewerGuardianId);
                    return new MemoryWithCreator(memory, creatorName, creatorRole, isMine);
                })
                .toList();
    }
}
