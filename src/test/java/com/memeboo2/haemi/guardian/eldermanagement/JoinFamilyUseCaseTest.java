package com.memeboo2.haemi.guardian.eldermanagement;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.family.application.FamilyJoinSaver;
import com.memeboo2.haemi.guardian.family.application.FamilyProperties;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JoinFamilyUseCaseTest {

    @Mock FamilyRepository familyRepository;
    @Mock ElderRepository elderRepository;
    @Mock GuardianElderLinkRepository linkRepository;
    @Mock FamilyProperties props;

    // 합류 본문은 REQUIRES_NEW 저장자에 있다 — 여기서 직접 검증한다.
    @InjectMocks FamilyJoinSaver familyJoinSaver;

    UUID guardianId = UUID.randomUUID();
    String inviteCode = "ABCD2345";

    @Test
    void 정상_합류_시_어르신_링크_자동_생성() {
        Family family = Family.create("테스트 가족", inviteCode);
        UUID familyId = family.getId();
        Elder elder1 = Elder.create(UUID.randomUUID(), familyId, "어르신1", null);
        Elder elder2 = Elder.create(UUID.randomUUID(), familyId, "어르신2", null);

        given(familyRepository.findByInviteCodeForUpdate(inviteCode)).willReturn(Optional.of(family));
        given(familyRepository.findByMembers_UserId(guardianId)).willReturn(Optional.empty());
        given(props.maxGuardians()).willReturn(8);
        given(elderRepository.findAllByFamilyId(familyId)).willReturn(List.of(elder1, elder2));

        familyJoinSaver.join(guardianId, inviteCode);

        // 어르신 수만큼 링크 생성
        verify(linkRepository, times(2)).save(any());
    }

    @Test
    void 유효하지_않은_초대코드는_404() {
        given(familyRepository.findByInviteCodeForUpdate(inviteCode)).willReturn(Optional.empty());

        assertThatThrownBy(() -> familyJoinSaver.join(guardianId, inviteCode))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void 이미_가족에_속한_보호자는_예외() {
        Family otherFamily = Family.create("다른 가족", "OTHER1234");
        given(familyRepository.findByInviteCodeForUpdate(inviteCode))
                .willReturn(Optional.of(Family.create("이 가족", inviteCode)));
        given(familyRepository.findByMembers_UserId(guardianId)).willReturn(Optional.of(otherFamily));

        assertThatThrownBy(() -> familyJoinSaver.join(guardianId, inviteCode))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FAMILY_CAPACITY_EXCEEDED));
    }

    @Test
    void 동시_합류로_uk_family_member_user_위반이_나면_409로_변환된다() {
        Family family = Family.create("테스트 가족", inviteCode);
        given(familyRepository.findByInviteCodeForUpdate(inviteCode)).willReturn(Optional.of(family));
        given(familyRepository.findByMembers_UserId(guardianId)).willReturn(Optional.empty());
        given(props.maxGuardians()).willReturn(8);
        // 선검사를 통과한 뒤 flush에서 유니크 제약이 잡는 경쟁 상황.
        willThrow(new DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint \"uk_family_member_user\""))
                .given(familyRepository).flush();

        assertThatThrownBy(() -> familyJoinSaver.join(guardianId, inviteCode))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FAMILY_CAPACITY_EXCEEDED));

        // 위반 시 링크를 만들지 않는다.
        verify(linkRepository, times(0)).save(any());
    }
}
