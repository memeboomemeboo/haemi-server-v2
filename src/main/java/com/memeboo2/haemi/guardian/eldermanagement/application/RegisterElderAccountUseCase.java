package com.memeboo2.haemi.guardian.eldermanagement.application;

import com.memeboo2.haemi.auth.api.AccountCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/** 어르신 계정과 guardian_elders 레코드를 하나의 원자적 등록으로 묶는다. */
@Service
@RequiredArgsConstructor
public class RegisterElderAccountUseCase {

    private final AccountCommand accountCommand;
    private final RegisterElderUseCase registerElderUseCase;

    @Transactional
    public UUID execute(UUID guardianId, UUID familyId, String name, LocalDate birthDate,
                        String loginId, String credential, String phone, String gender) {
        UUID accountId = accountCommand.createElderAccount(
                name, loginId, credential,
                birthDate == null ? null : birthDate.toString(), phone, gender);
        return registerElderUseCase.execute(guardianId, accountId, familyId, name, birthDate);
    }
}
