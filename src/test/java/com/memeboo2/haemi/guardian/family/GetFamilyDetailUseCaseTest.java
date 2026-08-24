package com.memeboo2.haemi.guardian.family;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.family.application.GetFamilyDetailUseCase;
import com.memeboo2.haemi.guardian.family.application.GetFamilyDetailUseCase.FamilyDetail;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetFamilyDetailUseCaseTest {

    @Mock FamilyRepository familyRepository;
    @Mock ElderRepository elderRepository;
    @Mock GuardianElderLinkRepository linkRepository;
    @Mock AccountQuery accountQuery;
    @InjectMocks GetFamilyDetailUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID otherGuardianId = UUID.randomUUID();

    @Test
    void 소속_가족이_없으면_빈값을_반환한다() {
        given(familyRepository.findByMembers_UserId(guardianId)).willReturn(Optional.empty());

        assertThat(useCase.execute(guardianId, null)).isEmpty();
    }

    @Test
    void 어르신이_1명이면_지정하지_않아도_다른_보호자_관계가_채워진다() {
        Family family = Family.create("우리가족", "INVITE01");
        family.addMember(guardianId);
        family.addMember(otherGuardianId);
        ReflectionTestUtils.setField(family, "id", UUID.randomUUID());
        Elder elder = Elder.create(UUID.randomUUID(), family.getId(), "어르신", LocalDate.of(1950, 1, 1));
        ReflectionTestUtils.setField(elder, "id", UUID.randomUUID());

        given(familyRepository.findByMembers_UserId(guardianId)).willReturn(Optional.of(family));
        given(elderRepository.findAllByFamilyId(family.getId())).willReturn(List.of(elder));
        given(accountQuery.findById(guardianId))
                .willReturn(Optional.of(new AccountQuery.AccountInfo(guardianId, "나", "id1", "010", null, null)));
        given(accountQuery.findById(otherGuardianId))
                .willReturn(Optional.of(new AccountQuery.AccountInfo(otherGuardianId, "동생", "id2", "010", null, null)));
        GuardianElderLink otherLink = GuardianElderLink.create(otherGuardianId, elder.getId());
        otherLink.changeRole(GuardianRole.SON);
        given(linkRepository.findByGuardianIdAndElderId(otherGuardianId, elder.getId()))
                .willReturn(Optional.of(otherLink));
        given(linkRepository.findByGuardianIdAndElderId(guardianId, elder.getId())).willReturn(Optional.empty());

        FamilyDetail detail = useCase.execute(guardianId, null).orElseThrow();

        assertThat(detail.inviteCode()).isEqualTo("INVITE01");
        assertThat(detail.guardians()).hasSize(2);
        var other = detail.guardians().stream().filter(g -> !g.isMe()).findFirst().orElseThrow();
        assertThat(other.name()).isEqualTo("동생");
        assertThat(other.role()).isEqualTo(GuardianRole.SON);
    }

    @Test
    void 어르신이_2명_이상이고_기준을_지정하지_않으면_role은_null이다() {
        Family family = Family.create("우리가족", "INVITE02");
        family.addMember(guardianId);
        family.addMember(otherGuardianId);
        Elder elder1 = Elder.create(UUID.randomUUID(), family.getId(), "어르신1", null);
        Elder elder2 = Elder.create(UUID.randomUUID(), family.getId(), "어르신2", null);

        given(familyRepository.findByMembers_UserId(guardianId)).willReturn(Optional.of(family));
        given(elderRepository.findAllByFamilyId(family.getId())).willReturn(List.of(elder1, elder2));
        given(accountQuery.findById(guardianId))
                .willReturn(Optional.of(new AccountQuery.AccountInfo(guardianId, "나", "id1", "010", null, null)));
        given(accountQuery.findById(otherGuardianId))
                .willReturn(Optional.of(new AccountQuery.AccountInfo(otherGuardianId, "동생", "id2", "010", null, null)));
        given(linkRepository.findByGuardianIdAndElderId(guardianId, elder1.getId())).willReturn(Optional.empty());
        given(linkRepository.findByGuardianIdAndElderId(guardianId, elder2.getId())).willReturn(Optional.empty());

        FamilyDetail detail = useCase.execute(guardianId, null).orElseThrow();

        assertThat(detail.guardians()).allSatisfy(g -> assertThat(g.role()).isNull());
    }
}
