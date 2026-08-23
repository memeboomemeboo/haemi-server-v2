package com.memeboo2.haemi.guardian.eldermanagement.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnlinkGuardianUseCase {

    private final GuardianElderLinkRepository linkRepository;

    /**
     * 보호자-어르신 링크 해제 (R8).
     * actor: 요청한 보호자 — 본인 링크만 해제 가능.
     * 마지막 보호자는 해제 불가 (고아 계정 방지).
     */
    @Transactional
    public void execute(UUID actorId, UUID elderId) {
        var links = linkRepository.findAllByElderIdForUpdate(elderId);
        var link = links.stream().filter(candidate -> candidate.getGuardianId().equals(actorId))
                .findFirst()
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_RESOURCE_OWNER,
                        "본인 링크만 해제할 수 있습니다."));

        long remaining = links.size();
        if (remaining <= 1) {
            throw new DomainException(ErrorCode.LAST_GUARDIAN_CANNOT_LEAVE);
        }

        linkRepository.delete(link);
    }
}
