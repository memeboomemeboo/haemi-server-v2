package com.memeboo2.haemi.guardian.profile;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.application.ChangeGuardianRoleUseCase;
import com.memeboo2.haemi.guardian.profile.application.UpdateGuardianProfileUseCase;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateGuardianProfileUseCaseTest {

    @Mock AccountQuery accountQuery;
    @Mock ChangeGuardianRoleUseCase changeGuardianRoleUseCase;
    @Mock MediaUploadCommand mediaUploadCommand;
    @InjectMocks UpdateGuardianProfileUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId = UUID.randomUUID();

    @Test
    void 역할_변경은_ChangeGuardianRoleUseCase에_위임한다() {
        useCase.execute(guardianId, null, null, Map.of(elderId, GuardianRole.딸));

        verify(changeGuardianRoleUseCase).execute(guardianId, elderId, GuardianRole.딸);
    }

    @Test
    void 역할이_null이면_400() {
        Map<UUID, GuardianRole> roles = new java.util.HashMap<>();
        roles.put(elderId, null);

        assertThatThrownBy(() -> useCase.execute(guardianId, null, null, roles))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void 본인_링크가_아니면_NOT_RESOURCE_OWNER를_그대로_전파한다() {
        willThrow(new DomainException(ErrorCode.NOT_RESOURCE_OWNER))
                .given(changeGuardianRoleUseCase).execute(guardianId, elderId, GuardianRole.딸);

        assertThatThrownBy(() -> useCase.execute(guardianId, null, null, Map.of(elderId, GuardianRole.딸)))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_RESOURCE_OWNER));
    }
}
