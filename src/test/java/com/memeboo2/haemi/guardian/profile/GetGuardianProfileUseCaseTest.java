package com.memeboo2.haemi.guardian.profile;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLink;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.profile.application.GetGuardianProfileUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetGuardianProfileUseCaseTest {

    @Mock AccountQuery accountQuery;
    @Mock GuardianElderLinkRepository linkRepository;
    @Mock ElderRepository elderRepository;
    @InjectMocks GetGuardianProfileUseCase useCase;

    UUID guardianId = UUID.randomUUID();
    UUID elderId1 = UUID.randomUUID();
    UUID elderId2 = UUID.randomUUID();

    private AccountQuery.AccountInfo accountInfo() {
        return new AccountQuery.AccountInfo(guardianId, "황정빈", "hjbin1211", "010-1234-5678",
                "1999-01-01", "https://image.example/profile.png", null);
    }

    private GuardianElderLink linkFor(UUID elderId) throws Exception {
        GuardianElderLink link = GuardianElderLink.create(guardianId, elderId);
        Field elderIdField = GuardianElderLink.class.getDeclaredField("elderId");
        elderIdField.setAccessible(true);
        elderIdField.set(link, elderId);
        return link;
    }

    private Elder elder(UUID elderId, String name) throws Exception {
        Elder elder = Elder.create(UUID.randomUUID(), UUID.randomUUID(), name, LocalDate.of(1950, 1, 1));
        Field idField = com.memeboo2.haemi.common.persistence.BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(elder, elderId);
        return elder;
    }

    @Test
    void 정상_경로_어르신_카드_포함() throws Exception {
        given(accountQuery.findById(guardianId)).willReturn(Optional.of(accountInfo()));
        GuardianElderLink link = linkFor(elderId1);
        given(linkRepository.findAllByGuardianId(guardianId)).willReturn(List.of(link));
        Elder elder = elder(elderId1, "황영수");
        given(elderRepository.findById(elderId1)).willReturn(Optional.of(elder));

        GetGuardianProfileUseCase.GuardianProfile result = useCase.execute(guardianId);

        assertThat(result.userId()).isEqualTo(guardianId);
        assertThat(result.name()).isEqualTo("황정빈");
        assertThat(result.elders()).hasSize(1);
        assertThat(result.elders().get(0).elderId()).isEqualTo(elderId1);
        assertThat(result.elders().get(0).name()).isEqualTo("황영수");
        assertThat(result.elders().get(0).role()).isEqualTo(GuardianRole.GUARDIAN);
    }

    @Test
    void 계정_없으면_RESOURCE_NOT_FOUND() {
        given(accountQuery.findById(guardianId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(guardianId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void 링크는_있지만_어르신_삭제된_경우_필터링() throws Exception {
        given(accountQuery.findById(guardianId)).willReturn(Optional.of(accountInfo()));
        GuardianElderLink link1 = linkFor(elderId1);
        GuardianElderLink link2 = linkFor(elderId2);
        given(linkRepository.findAllByGuardianId(guardianId)).willReturn(List.of(link1, link2));
        given(elderRepository.findById(elderId1)).willReturn(Optional.empty());
        Elder elder2 = elder(elderId2, "황영수");
        given(elderRepository.findById(elderId2)).willReturn(Optional.of(elder2));

        GetGuardianProfileUseCase.GuardianProfile result = useCase.execute(guardianId);

        assertThat(result.elders()).hasSize(1);
        assertThat(result.elders().get(0).elderId()).isEqualTo(elderId2);
    }
}
