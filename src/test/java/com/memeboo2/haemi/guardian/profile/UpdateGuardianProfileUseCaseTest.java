package com.memeboo2.haemi.guardian.profile;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.application.ChangeGuardianRoleUseCase;
import com.memeboo2.haemi.guardian.profile.application.UpdateGuardianProfileUseCase;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UpdateGuardianProfileUseCaseTest {

    @Mock AccountQuery accountQuery;
    @Mock ChangeGuardianRoleUseCase changeGuardianRoleUseCase;
    @Mock MediaUploadCommand mediaUploadCommand;
    @Mock HaemiClock clock;
    @InjectMocks UpdateGuardianProfileUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId = UUID.randomUUID();

    private AccountQuery.AccountInfo accountInfo(String loginId) {
        return new AccountQuery.AccountInfo(guardianId, "황정빈", loginId, "010-1234-5678",
                "1999-01-01", null, null);
    }

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(clock.today()).thenReturn(LocalDate.of(2026, 8, 30));
    }

    @Test
    void loginId_변경되고_사용가능하면_업데이트() {
        given(accountQuery.findById(guardianId)).willReturn(Optional.of(accountInfo("oldLoginId")));
        given(accountQuery.existsByLoginId("newLoginId")).willReturn(false);

        useCase.execute(guardianId, null, null, null, "newLoginId", null, Map.of());

        then(accountQuery).should().updateLoginId(guardianId, "newLoginId");
    }

    @Test
    void loginId_이미_사용중이면_예외() {
        given(accountQuery.findById(guardianId)).willReturn(Optional.of(accountInfo("oldLoginId")));
        given(accountQuery.existsByLoginId("newLoginId")).willReturn(true);

        assertThatThrownBy(() -> useCase.execute(guardianId, null, null, null, "newLoginId", null, Map.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LOGIN_ID_ALREADY_TAKEN));

        then(accountQuery).should(org.mockito.Mockito.never()).updateLoginId(any(), any());
    }

    @Test
    void loginId가_null이면_변경_생략() {
        useCase.execute(guardianId, null, null, null, null, null, Map.of());

        then(accountQuery).should(org.mockito.Mockito.never()).findById(any());
        then(accountQuery).should(org.mockito.Mockito.never()).updateLoginId(any(), any());
    }

    @Test
    void loginId가_기존과_동일하면_변경_생략() {
        given(accountQuery.findById(guardianId)).willReturn(Optional.of(accountInfo("sameLoginId")));

        useCase.execute(guardianId, null, null, null, "sameLoginId", null, Map.of());

        then(accountQuery).should(org.mockito.Mockito.never()).existsByLoginId(any());
        then(accountQuery).should(org.mockito.Mockito.never()).updateLoginId(any(), any());
    }

    @Test
    void elderRole값이_null이면_INVALID_INPUT() {
        Map<UUID, GuardianRole> elderRoles = new HashMap<>();
        elderRoles.put(elderId, null);

        assertThatThrownBy(() -> useCase.execute(guardianId, null, null, null, null, null, elderRoles))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void mediaRefId_있으면_프로필_이미지_업데이트() {
        UUID mediaRefId = UUID.randomUUID();
        given(mediaUploadCommand.confirmUploadKey(guardianId, mediaRefId, MediaPurpose.PROFILE_IMAGE))
                .willReturn("profile_image/confirmed.png");

        useCase.execute(guardianId, null, null, null, null, mediaRefId, Map.of());

        then(accountQuery).should().updateProfileImageUrl(guardianId, "profile_image/confirmed.png");
    }

    @Test
    void 이름과_생년월일과_전화번호를_함께_수정한다() {
        useCase.execute(guardianId, "박승아", LocalDate.of(1985, 6, 10), "010-9999-8888", null, null, Map.of());

        then(accountQuery).should().updateName(guardianId, "박승아");
        then(accountQuery).should().updateBirthDate(guardianId, "1985-06-10");
        then(accountQuery).should().updatePhone(guardianId, "010-9999-8888");
    }

    @Test
    void 생년월일이_1920년보다_빠르면_거절한다() {
        assertThatThrownBy(() -> useCase.execute(
                guardianId, "박승아", LocalDate.of(1919, 12, 31), null, null, null, Map.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));

        then(accountQuery).should(org.mockito.Mockito.never()).updateName(any(), any());
        then(accountQuery).should(org.mockito.Mockito.never()).updateBirthDate(any(), any());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
