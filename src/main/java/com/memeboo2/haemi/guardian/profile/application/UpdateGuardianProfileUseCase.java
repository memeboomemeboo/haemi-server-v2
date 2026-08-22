package com.memeboo2.haemi.guardian.profile.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateGuardianProfileUseCase {

    private final AccountQuery accountQuery;
    private final GuardianElderLinkRepository linkRepository;

    /**
     * @param newLoginId null이면 변경 안 함
     * @param elderRoles elderId → 변경할 역할 (빈 맵이면 역할 변경 없음)
     */
    @Transactional
    public void execute(UUID guardianId, String newLoginId, Map<UUID, GuardianRole> elderRoles) {
        if (newLoginId != null) {
            AccountQuery.AccountInfo account = accountQuery.findById(guardianId)
                    .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));

            if (!newLoginId.equals(account.loginId())) {
                if (accountQuery.existsByLoginId(newLoginId)) {
                    throw new DomainException(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
                }
                accountQuery.updateLoginId(guardianId, newLoginId);
            }
        }

        elderRoles.forEach((elderId, role) ->
                linkRepository.findByGuardianIdAndElderId(guardianId, elderId)
                        .ifPresent(link -> link.changeRole(role))
        );
    }
}
