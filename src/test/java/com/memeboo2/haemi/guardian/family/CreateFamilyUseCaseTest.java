package com.memeboo2.haemi.guardian.family;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.family.application.CreateFamilyUseCase;
import com.memeboo2.haemi.guardian.family.application.FamilyProperties;
import com.memeboo2.haemi.guardian.family.application.InviteCodeGenerator;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CreateFamilyUseCaseTest {

    @Mock FamilyRepository familyRepository;
    @Mock FamilyProperties props;
    @Mock MediaUploadCommand mediaUploadCommand;
    @Mock InviteCodeGenerator inviteCodeGenerator;
    @InjectMocks CreateFamilyUseCase useCase;

    UUID guardianId = UUID.randomUUID();

    @Test
    void 정상_생성시_초대코드를_함께_반환한다() {
        given(familyRepository.findByMembers_UserId(guardianId)).willReturn(Optional.empty());
        given(inviteCodeGenerator.nextCode()).willReturn("ABCD2345");
        given(familyRepository.existsByInviteCode("ABCD2345")).willReturn(false);
        given(familyRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        CreateFamilyUseCase.Result result = useCase.execute(guardianId, "우리가족", null, null);

        assertThat(result.inviteCode()).isEqualTo("ABCD2345");
    }

    @Test
    void 초대코드가_중복되면_재생성한다() {
        given(familyRepository.findByMembers_UserId(guardianId)).willReturn(Optional.empty());
        given(inviteCodeGenerator.nextCode()).willReturn("DUPCODE1", "FRESHCOD");
        given(familyRepository.existsByInviteCode("DUPCODE1")).willReturn(true);
        given(familyRepository.existsByInviteCode("FRESHCOD")).willReturn(false);
        given(familyRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        CreateFamilyUseCase.Result result = useCase.execute(guardianId, "우리가족", null, null);

        assertThat(result.inviteCode()).isEqualTo("FRESHCOD");
    }

    @Test
    void 이미_가족에_속한_보호자는_409() {
        given(familyRepository.findByMembers_UserId(guardianId))
                .willReturn(Optional.of(Family.create("기존 가족", "EXIST123")));

        assertThatThrownBy(() -> useCase.execute(guardianId, "우리가족", null, null))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FAMILY_CAPACITY_EXCEEDED));
    }
}
