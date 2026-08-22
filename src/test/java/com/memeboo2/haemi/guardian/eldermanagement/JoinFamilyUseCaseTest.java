package com.memeboo2.haemi.guardian.eldermanagement;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.family.application.FamilyProperties;
import com.memeboo2.haemi.guardian.family.application.JoinFamilyUseCase;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JoinFamilyUseCaseTest {

    @Mock FamilyRepository familyRepository;
    @Mock ElderRepository elderRepository;
    @Mock GuardianElderLinkRepository linkRepository;
    @Mock FamilyProperties props;

    @InjectMocks JoinFamilyUseCase joinFamilyUseCase;

    UUID guardianId = UUID.randomUUID();
    UUID familyId   = UUID.randomUUID();

    @Test
    void 정상_합류_시_어르신_링크_자동_생성() {
        Family family = Family.create("테스트 가족");
        Elder elder1 = Elder.create(UUID.randomUUID(), familyId, "어르신1", null);
        Elder elder2 = Elder.create(UUID.randomUUID(), familyId, "어르신2", null);

        given(familyRepository.findById(familyId)).willReturn(Optional.of(family));
        given(familyRepository.findByMembers_UserId(guardianId)).willReturn(Optional.empty());
        given(props.maxGuardians()).willReturn(8);
        given(elderRepository.findAllByFamilyId(familyId)).willReturn(List.of(elder1, elder2));

        joinFamilyUseCase.execute(guardianId, familyId);

        // 어르신 수만큼 링크 생성
        verify(linkRepository, times(2)).save(any());
    }

    @Test
    void 링크없는_보호자는_403() {
        given(familyRepository.findById(familyId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> joinFamilyUseCase.execute(guardianId, familyId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex ->
                        org.assertj.core.api.Assertions.assertThat(((DomainException) ex).getErrorCode())
                                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void 이미_가족에_속한_보호자는_예외() {
        Family otherFamily = Family.create("다른 가족");
        given(familyRepository.findById(familyId)).willReturn(Optional.of(Family.create("이 가족")));
        given(familyRepository.findByMembers_UserId(guardianId)).willReturn(Optional.of(otherFamily));

        assertThatThrownBy(() -> joinFamilyUseCase.execute(guardianId, familyId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex ->
                        org.assertj.core.api.Assertions.assertThat(((DomainException) ex).getErrorCode())
                                .isEqualTo(ErrorCode.FAMILY_CAPACITY_EXCEEDED));
    }
}
