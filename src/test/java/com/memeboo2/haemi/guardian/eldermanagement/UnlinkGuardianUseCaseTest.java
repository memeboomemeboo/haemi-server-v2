package com.memeboo2.haemi.guardian.eldermanagement;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.eldermanagement.application.UnlinkGuardianUseCase;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnlinkGuardianUseCaseTest {

    @Mock GuardianElderLinkRepository linkRepository;
    @InjectMocks UnlinkGuardianUseCase unlinkUseCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId    = UUID.randomUUID();

    @Test
    void 정상_경로_링크_해제() {
        GuardianElderLink link = GuardianElderLink.create(guardianId, elderId);
        given(linkRepository.findAllByElderIdForUpdate(elderId)).willReturn(List.of(
                link, GuardianElderLink.create(UUID.randomUUID(), elderId)));

        unlinkUseCase.execute(guardianId, elderId);

        verify(linkRepository).delete(link);
    }

    @Test
    void 링크없는_보호자는_403() {
        given(linkRepository.findAllByElderIdForUpdate(elderId)).willReturn(List.of(
                GuardianElderLink.create(UUID.randomUUID(), elderId)));

        assertThatThrownBy(() -> unlinkUseCase.execute(guardianId, elderId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_RESOURCE_OWNER));
    }

    @Test
    void 마지막_보호자는_해제_불가() {
        GuardianElderLink link = GuardianElderLink.create(guardianId, elderId);
        given(linkRepository.findAllByElderIdForUpdate(elderId)).willReturn(List.of(link));

        assertThatThrownBy(() -> unlinkUseCase.execute(guardianId, elderId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.LAST_GUARDIAN_CANNOT_LEAVE));
    }
}
