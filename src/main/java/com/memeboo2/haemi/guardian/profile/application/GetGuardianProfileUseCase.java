package com.memeboo2.haemi.guardian.profile.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetGuardianProfileUseCase {

    private final AccountQuery accountQuery;
    private final FamilyRepository familyRepository;
    private final GuardianElderLinkRepository linkRepository;
    private final ElderRepository elderRepository;

    public record ElderCard(UUID elderId, String name, LocalDate birthDate, GuardianRole role) {}

    public record FamilyInfo(UUID familyId, String name, String memo, String profileImageUrl) {}

    public record GuardianProfile(
            UUID userId,
            String name,
            String loginId,
            String phone,
            String birthDate,
            String profileImageUrl,
            FamilyInfo family,
            List<ElderCard> elders
    ) {}

    @Transactional(readOnly = true)
    public GuardianProfile execute(UUID guardianId) {
        AccountQuery.AccountInfo account = accountQuery.findById(guardianId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));

        Optional<Family> family = familyRepository.findByMembers_UserId(guardianId);
        FamilyInfo familyInfo = family.map(f -> new FamilyInfo(
                f.getId(), f.getName(), f.getMemo(), f.getProfileImageUrl())).orElse(null);

        List<ElderCard> elders = linkRepository.findAllByGuardianId(guardianId).stream()
                .map(link -> {
                    Elder elder = elderRepository.findById(link.getElderId()).orElse(null);
                    if (elder == null) return null;
                    return new ElderCard(elder.getId(), elder.getName(), elder.getBirthDate(), link.getRole());
                })
                .filter(c -> c != null)
                .toList();

        return new GuardianProfile(
                guardianId,
                account.name(),
                account.loginId(),
                account.phone(),
                account.birthDate(),
                account.profileImageUrl(),
                familyInfo,
                elders
        );
    }
}
