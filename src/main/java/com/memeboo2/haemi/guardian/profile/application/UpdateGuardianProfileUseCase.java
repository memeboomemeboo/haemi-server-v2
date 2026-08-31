package com.memeboo2.haemi.guardian.profile.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.application.ChangeGuardianRoleUseCase;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateGuardianProfileUseCase {

    private static final LocalDate MIN_BIRTH_DATE = LocalDate.of(1920, 1, 1);

    private final AccountQuery accountQuery;
    private final ChangeGuardianRoleUseCase changeGuardianRoleUseCase;
    private final MediaUploadCommand mediaUploadCommand;
    private final HaemiClock clock;

    /**
     * @param newName null이면 변경 안 함
     * @param newBirthDate null이면 변경 안 함
     * @param newPhone null이면 변경 안 함
     * @param newLoginId null이면 변경 안 함
     * @param elderRoles elderId → 변경할 역할 (빈 맵이면 역할 변경 없음)
     */
    @Transactional
    public void execute(UUID guardianId, String newName, LocalDate newBirthDate,
                        String newPhone, String newLoginId, UUID profileImageMediaRefId,
                        Map<UUID, GuardianRole> elderRoles) {
        if (newBirthDate != null) {
            validateBirthDate(newBirthDate);
        }

        if (newName != null) {
            accountQuery.updateName(guardianId, newName);
        }

        if (newBirthDate != null) {
            accountQuery.updateBirthDate(guardianId, newBirthDate.toString());
        }

        if (newPhone != null) {
            accountQuery.updatePhone(guardianId, newPhone);
        }

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
                    .confirmUploadKey(guardianId, profileImageMediaRefId, MediaPurpose.PROFILE_IMAGE);
            accountQuery.updateProfileImageUrl(guardianId, profileImageUrl);
        }

        elderRoles.forEach((elderId, role) -> {
            if (role == null) {
                throw new DomainException(ErrorCode.INVALID_INPUT, "보호자 역할을 입력해주세요.");
            }
            changeGuardianRoleUseCase.execute(guardianId, elderId, role);
        });
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate.isBefore(MIN_BIRTH_DATE) || birthDate.isAfter(clock.today())) {
            throw new DomainException(ErrorCode.INVALID_INPUT,
                    "생년월일은 1920년 1월 1일부터 오늘까지 선택할 수 있습니다.");
        }
    }
}
