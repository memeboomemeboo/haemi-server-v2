package com.memeboo2.haemi.guardian.family.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateFamilyUseCase {

    private final FamilyRepository familyRepository;
    private final FamilyProperties props;

    @Transactional
    public UUID execute(UUID guardianId, String familyName) {
        // 보호자는 한 가족에만 속할 수 있음 (R2)
        familyRepository.findByMembers_UserId(guardianId).ifPresent(f -> {
            throw new DomainException(ErrorCode.FAMILY_CAPACITY_EXCEEDED,
                    "이미 가족에 속해 있습니다.");
        });

        Family family = Family.create(familyName);
        family.addMember(guardianId);
        return familyRepository.save(family).getId();
    }
}
