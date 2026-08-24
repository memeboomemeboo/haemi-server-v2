package com.memeboo2.haemi.guardian.family.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
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

/**
 * GET /families/my 단일 조회 (D10). 화면 분할은 프론트가 담당한다.
 */
@Service
@RequiredArgsConstructor
public class GetFamilyDetailUseCase {

    private final FamilyRepository familyRepository;
    private final ElderRepository elderRepository;
    private final GuardianElderLinkRepository linkRepository;
    private final AccountQuery accountQuery;

    public record GuardianMember(UUID userId, String name, GuardianRole role, boolean isMe) {}

    public record ElderCard(UUID elderId, String name, LocalDate birthDate, GuardianRole myRole) {}

    public record FamilyDetail(
            UUID familyId,
            String name,
            String memo,
            String profileImageUrl,
            String inviteCode,
            List<GuardianMember> guardians,
            List<ElderCard> elders
    ) {}

    /**
     * @param contextElderId 다른 보호자의 관계 라벨을 어느 어르신 기준으로 볼지 (Q8-1-(3)).
     *                        null이면 어르신이 정확히 1명일 때만 그 어르신을 기준으로 하고,
     *                        0명이거나 2명 이상이면 role은 null로 내려간다.
     */
    @Transactional(readOnly = true)
    public Optional<FamilyDetail> execute(UUID guardianId, UUID contextElderId) {
        Family family = familyRepository.findByMembers_UserId(guardianId).orElse(null);
        if (family == null) {
            return Optional.empty();
        }

        List<Elder> elderEntities = elderRepository.findAllByFamilyId(family.getId());
        UUID resolvedContextElderId = contextElderId != null ? contextElderId
                : elderEntities.size() == 1 ? elderEntities.get(0).getId() : null;

        List<GuardianMember> guardians = family.getMembers().stream()
                .filter(m -> !m.isElder())
                .map(m -> {
                    String name = accountQuery.findById(m.getUserId())
                            .map(AccountQuery.AccountInfo::name).orElse(null);
                    GuardianRole role = resolvedContextElderId == null ? null
                            : linkRepository.findByGuardianIdAndElderId(m.getUserId(), resolvedContextElderId)
                                    .map(link -> link.getRole()).orElse(null);
                    return new GuardianMember(m.getUserId(), name, role, m.getUserId().equals(guardianId));
                })
                .toList();

        List<ElderCard> elders = elderEntities.stream()
                .map(elder -> new ElderCard(
                        elder.getId(), elder.getName(), elder.getBirthDate(),
                        linkRepository.findByGuardianIdAndElderId(guardianId, elder.getId())
                                .map(link -> link.getRole()).orElse(null)))
                .toList();

        return Optional.of(new FamilyDetail(
                family.getId(), family.getName(), family.getMemo(), family.getProfileImageUrl(),
                family.getInviteCode(), guardians, elders));
    }
}
