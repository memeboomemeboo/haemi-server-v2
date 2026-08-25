package com.memeboo2.haemi.guardian.family.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateFamilyUseCase {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final FamilyRepository familyRepository;
    private final FamilyProperties props;
    private final MediaUploadCommand mediaUploadCommand;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final FamilyInviteCodeSaver familyInviteCodeSaver;

    public record Result(UUID familyId, String inviteCode) {}

    @Transactional
    public Result execute(UUID guardianId, String familyName, String memo, UUID profileImageMediaRefId) {
        // 보호자는 한 가족에만 속할 수 있음 (R2)
        familyRepository.findByMembers_UserId(guardianId).ifPresent(f -> {
            throw new DomainException(ErrorCode.FAMILY_CAPACITY_EXCEEDED,
                    "이미 가족에 속해 있습니다.");
        });

        String profileImageUrl = profileImageMediaRefId == null ? null
                : mediaUploadCommand.confirmUpload(guardianId, profileImageMediaRefId, MediaPurpose.PROFILE_IMAGE).toString();

        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String inviteCode = inviteCodeGenerator.nextCode();
            Family family = Family.create(familyName, memo, profileImageUrl, inviteCode);
            family.addMember(guardianId);
            if (familyInviteCodeSaver.trySave(family)) {
                return new Result(family.getId(), inviteCode);
            }
        }
        throw new IllegalStateException("초대 코드 생성에 반복적으로 실패했습니다.");
    }
}
