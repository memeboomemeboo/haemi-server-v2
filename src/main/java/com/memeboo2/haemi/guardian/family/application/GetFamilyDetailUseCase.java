package com.memeboo2.haemi.guardian.family.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyMember;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final MediaUploadCommand mediaUploadCommand;

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

        List<UUID> memberIds = family.getMembers().stream()
                .filter(m -> !m.isElder())
                .map(FamilyMember::getUserId)
                .toList();

        // 계정 이름 일괄 조회 (N+1 방지)
        Map<UUID, String> nameByUserId = accountQuery.findAllById(memberIds).stream()
                .collect(Collectors.toMap(AccountQuery.AccountInfo::userId, AccountQuery.AccountInfo::name));

        // 컨텍스트 어르신 기준 보호자별 관계 라벨 일괄 조회 (N+1 방지)
        Map<UUID, GuardianRole> roleByGuardianId = resolvedContextElderId == null ? Map.of()
                : linkRepository.findAllByGuardianIdInAndElderId(memberIds, resolvedContextElderId).stream()
                        .collect(Collectors.toMap(GuardianElderLink::getGuardianId, GuardianElderLink::getRole));

        List<GuardianMember> guardians = memberIds.stream()
                .map(userId -> new GuardianMember(
                        userId, nameByUserId.get(userId),
                        roleByGuardianId.get(userId), userId.equals(guardianId)))
                .toList();

        // 요청 보호자의 어르신별 관계 라벨 일괄 조회 (N+1 방지)
        Map<UUID, GuardianRole> myRoleByElderId = linkRepository.findAllByGuardianId(guardianId).stream()
                .collect(Collectors.toMap(GuardianElderLink::getElderId, GuardianElderLink::getRole));

        List<ElderCard> elders = elderEntities.stream()
                .map(elder -> new ElderCard(
                        elder.getId(), elder.getName(), elder.getBirthDate(),
                        myRoleByElderId.get(elder.getId())))
                .toList();

        return Optional.of(new FamilyDetail(
                family.getId(), family.getName(), family.getMemo(), mediaUploadCommand.resolveServingUrl(family.getProfileImageUrl()),
                family.getInviteCode(), guardians, elders));
    }
}
