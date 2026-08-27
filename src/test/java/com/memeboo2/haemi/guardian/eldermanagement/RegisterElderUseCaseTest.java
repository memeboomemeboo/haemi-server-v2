package com.memeboo2.haemi.guardian.eldermanagement;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.eldermanagement.application.RegisterElderUseCase;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import com.memeboo2.haemi.guardian.family.application.FamilyProperties;
import com.memeboo2.haemi.guardian.family.domain.Family;
import com.memeboo2.haemi.guardian.family.domain.FamilyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RegisterElderUseCaseTest {

    @Mock ElderRepository elderRepository;
    @Mock GuardianElderLinkRepository linkRepository;
    @Mock FamilyRepository familyRepository;

    private final FamilyProperties props = new FamilyProperties(4, 8);
    private RegisterElderUseCase sut;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        sut = new RegisterElderUseCase(elderRepository, linkRepository, familyRepository, props);
    }

    @Test
    void 정상_어르신_등록() throws Exception {
        UUID guardianId = UUID.randomUUID();
        UUID elderUserId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        Family family = Family.create("우리가족", "INVITE1234");
        family.addMember(guardianId);
        given(familyRepository.findByIdForUpdate(familyId)).willReturn(Optional.of(family));
        given(elderRepository.findByUserId(elderUserId)).willReturn(Optional.empty());
        given(elderRepository.countByFamilyId(familyId)).willReturn(0L);
        UUID expectedId = UUID.randomUUID();
        given(elderRepository.save(any(Elder.class))).willAnswer(inv -> {
            Elder e = inv.getArgument(0);
            var idField = e.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(e, expectedId);
            return e;
        });

        UUID result = sut.execute(guardianId, elderUserId, familyId, "김할머니", LocalDate.of(1945, 1, 1));

        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    void 가족_구성원이_아니면_403() {
        UUID guardianId = UUID.randomUUID();
        UUID elderUserId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        Family family = Family.create("우리가족", "INVITE1234");
        given(familyRepository.findByIdForUpdate(familyId)).willReturn(Optional.of(family));

        assertThatThrownBy(() -> sut.execute(guardianId, elderUserId, familyId, "김할머니", LocalDate.of(1945, 1, 1)))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.CARE_ACCESS_DENIED);
    }
}
