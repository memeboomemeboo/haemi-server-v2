package com.memeboo2.haemi.guardian.eldermanagement;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.access.CareAccessQueryImpl;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CareAccessQueryTest {

    @Mock
    GuardianElderLinkRepository linkRepository;

    @InjectMocks
    CareAccessQueryImpl careAccessQuery;

    UUID guardianId = UUID.randomUUID();
    UUID elderId    = UUID.randomUUID();

    @Test
    void 정상_경로_링크가_있으면_통과() {
        given(linkRepository.existsByGuardianIdAndElderId(guardianId, elderId)).willReturn(true);

        // 예외 없이 통과
        careAccessQuery.requireGuardianOf(guardianId, elderId);
    }

    @Test
    void 링크없는_보호자는_403() {
        given(linkRepository.existsByGuardianIdAndElderId(guardianId, elderId)).willReturn(false);

        assertThatThrownBy(() -> careAccessQuery.requireGuardianOf(guardianId, elderId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));
    }

    @Test
    void 접근_가능한_어르신_목록_반환() {
        UUID elder1 = UUID.randomUUID();
        UUID elder2 = UUID.randomUUID();
        GuardianElderLink link1 = GuardianElderLink.create(guardianId, elder1);
        GuardianElderLink link2 = GuardianElderLink.create(guardianId, elder2);
        given(linkRepository.findAllByGuardianId(guardianId)).willReturn(List.of(link1, link2));

        List<UUID> result = careAccessQuery.accessibleElders(guardianId);

        assertThat(result).containsExactlyInAnyOrder(elder1, elder2);
    }

    @Test
    void roleOf_링크없으면_403() {
        given(linkRepository.findByGuardianIdAndElderId(guardianId, elderId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> careAccessQuery.roleOf(guardianId, elderId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CARE_ACCESS_DENIED));
    }

    @Test
    void roleOf_링크있으면_역할_반환() {
        GuardianElderLink link = GuardianElderLink.create(guardianId, elderId);
        given(linkRepository.findByGuardianIdAndElderId(guardianId, elderId))
                .willReturn(Optional.of(link));

        GuardianRole role = careAccessQuery.roleOf(guardianId, elderId);

        assertThat(role).isEqualTo(GuardianRole.GUARDIAN);
    }
}
