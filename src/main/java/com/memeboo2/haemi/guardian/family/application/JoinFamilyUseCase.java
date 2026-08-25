package com.memeboo2.haemi.guardian.family.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JoinFamilyUseCase {

    private final FamilyRepository familyRepository;
    private final ElderRepository elderRepository;
    private final GuardianElderLinkRepository linkRepository;
    private final FamilyProperties props;

    /**
     * 보호자가 초대 코드로 기존 가족에 합류 (D4). 가족의 모든 어르신에 대해 링크 자동 생성 (R3).
     * 어르신 계정은 SecurityConfig에서 /api/v1/guardian/** 자체를 호출할 수 없어 별도 검증이 불필요하다.
     */
    @Transactional
    public void execute(UUID guardianId, String inviteCode) {
        Family family = familyRepository.findByInviteCodeForUpdate(inviteCode)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 초대 코드입니다."));

        // 이미 다른 가족에 속해 있는지 (R2: 보호자는 한 가족만)
        familyRepository.findByMembers_UserId(guardianId).ifPresent(f -> {
            throw new DomainException(ErrorCode.FAMILY_CAPACITY_EXCEEDED, "이미 가족에 속해 있습니다.");
        });

        // 보호자 상한 검증 (R1)
        if (family.guardianCount() >= props.maxGuardians()) {
            throw new DomainException(ErrorCode.FAMILY_CAPACITY_EXCEEDED,
                    "가족당 보호자는 최대 " + props.maxGuardians() + "명까지 등록할 수 있습니다.");
        }

        family.addMember(guardianId);
        // 선검사만으로는 동시 합류/생성을 막지 못한다. DB 제약(uk_family_member_user)을 여기서 확인한다.
        try {
            familyRepository.flush();
        } catch (DataIntegrityViolationException alreadyInAnotherFamily) {
            throw new DomainException(ErrorCode.FAMILY_CAPACITY_EXCEEDED, "이미 가족에 속해 있습니다.");
        }

        // 가족의 모든 어르신에 대해 링크 자동 생성 (R3)
        elderRepository.findAllByFamilyId(family.getId()).forEach(elder ->
                linkRepository.save(GuardianElderLink.create(guardianId, elder.getId()))
        );
    }
}
