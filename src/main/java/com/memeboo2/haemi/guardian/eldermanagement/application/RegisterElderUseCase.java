package com.memeboo2.haemi.guardian.eldermanagement.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.family.application.FamilyProperties;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterElderUseCase {

    private final ElderRepository elderRepository;
    private final GuardianElderLinkRepository linkRepository;
    private final FamilyRepository familyRepository;
    private final FamilyProperties props;

    /**
     * 어르신 등록 (ACC-REG-002).
     * guardianId: 요청한 보호자, elderUserId: auth에서 생성된 어르신 User.id
     */
    @Transactional
    public UUID execute(UUID guardianId, UUID elderUserId, UUID familyId,
                        String name, LocalDate birthDate) {
        var family = familyRepository.findByIdForUpdate(familyId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "가족을 찾을 수 없습니다."));
        if (!family.hasMember(guardianId)) {
            throw new DomainException(ErrorCode.CARE_ACCESS_DENIED);
        }
        if (elderRepository.findByUserId(elderUserId).isPresent()) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "이미 등록된 어르신 계정입니다.");
        }

        // 가족 행 잠금 뒤 상한을 검증해 동시 등록도 직렬화한다.
        long currentElders = elderRepository.countByFamilyId(familyId);
        if (currentElders >= props.maxElders()) {
            throw new DomainException(ErrorCode.FAMILY_CAPACITY_EXCEEDED,
                    "가족당 어르신은 최대 " + props.maxElders() + "명까지 등록할 수 있습니다.");
        }

        Elder elder = Elder.create(elderUserId, familyId, name, birthDate);
        elder = elderRepository.save(elder);

        // 가족 내 모든 보호자에 대해 링크 자동 생성 (R3)
        List<UUID> guardianIds = family.getMembers().stream()
                .filter(m -> !m.isElder())
                .map(m -> m.getUserId())
                .toList();

        for (UUID gId : guardianIds) {
            linkRepository.save(GuardianElderLink.create(gId, elder.getId()));
        }

        return elder.getId();
    }
}
