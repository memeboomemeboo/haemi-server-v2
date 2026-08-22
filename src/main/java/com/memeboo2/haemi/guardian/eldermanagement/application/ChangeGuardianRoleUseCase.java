package com.memeboo2.haemi.guardian.eldermanagement.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangeGuardianRoleUseCase {

    private final GuardianElderLinkRepository linkRepository;

    /**
     * 어르신에 대한 본인 역할 라벨 변경 (딸/아들/손녀 …).
     * 본인 링크만 변경 가능.
     */
    @Transactional
    public void execute(UUID actorId, UUID elderId, GuardianRole newRole) {
        var link = linkRepository.findByGuardianIdAndElderId(actorId, elderId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_RESOURCE_OWNER,
                        "본인 링크만 변경할 수 있습니다."));
        link.changeRole(newRole);
    }
}
