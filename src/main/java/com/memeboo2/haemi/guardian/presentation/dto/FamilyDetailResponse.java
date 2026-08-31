package com.memeboo2.haemi.guardian.presentation.dto;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.family.application.GetFamilyDetailUseCase.ElderCard;
import com.memeboo2.haemi.guardian.family.application.GetFamilyDetailUseCase.FamilyDetail;
import com.memeboo2.haemi.guardian.family.application.GetFamilyDetailUseCase.GuardianMember;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FamilyDetailResponse(
        UUID familyId,
        String name,
        String memo,
        String profileImageUrl,
        String inviteCode,
        List<GuardianMemberResponse> guardians,
        List<ElderCardResponse> elders
) {
    public record GuardianMemberResponse(
            UUID userId,
            String name,
            @Schema(description = "이 보호자의 관계 라벨. 어르신이 2명 이상인데 기준 어르신(elderId)을 지정하지 않으면 null")
            GuardianRole role,
            String roleLabel,
            boolean isMe
    ) {
        static GuardianMemberResponse from(GuardianMember m) {
            return new GuardianMemberResponse(m.userId(), m.name(), m.role(),
                    m.role() == null ? null : m.role().getLabel(), m.isMe());
        }
    }

    @Schema(name = "FamilyElderCardResponse")
    public record ElderCardResponse(UUID elderId, String name, LocalDate birthDate, GuardianRole myRole, String myRoleLabel) {
        static ElderCardResponse from(ElderCard c) {
            return new ElderCardResponse(c.elderId(), c.name(), c.birthDate(), c.myRole(),
                    c.myRole() == null ? null : c.myRole().getLabel());
        }
    }

    public static FamilyDetailResponse from(FamilyDetail d) {
        return new FamilyDetailResponse(
                d.familyId(), d.name(), d.memo(), d.profileImageUrl(), d.inviteCode(),
                d.guardians().stream().map(GuardianMemberResponse::from).toList(),
                d.elders().stream().map(ElderCardResponse::from).toList()
        );
    }
}
