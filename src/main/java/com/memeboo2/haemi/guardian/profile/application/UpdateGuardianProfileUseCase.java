package com.memeboo2.haemi.guardian.profile.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import com.memeboo2.haemi.platform.api.MediaPurpose;
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
    private final MediaUploadCommand mediaUploadCommand;

    /**
     * @param newLoginId null이면 변경 안 함
     * @param elderRoles elderId → 변경할 역할 (빈 맵이면 역할 변경 없음)
     */
    @Transactional
    public void execute(UUID guardianId, String newLoginId, UUID profileImageMediaRefId,
                        Map<UUID, GuardianRole> elderRoles) {
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

        if (profileImageMediaRefId != null) {
            String profileImageUrl = mediaUploadCommand
                    .confirmUpload(guardianId, profileImageMediaRefId, MediaPurpose.PROFILE_IMAGE).toString();
            accountQuery.updateProfileImageUrl(guardianId, profileImageUrl);
        }

        elderRoles.forEach((elderId, role) ->
                linkRepository.findByGuardianIdAndElderId(guardianId, elderId)
                        .ifPresentOrElse(link -> {
                            if (role == null) {
                                throw new DomainException(ErrorCode.INVALID_INPUT, "보호자 역할을 입력해주세요.");
                            }
                            link.changeRole(role);
                        }, () -> {
                            throw new DomainException(ErrorCode.CARE_ACCESS_DENIED);
                        })
        );
    }
}
