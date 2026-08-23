package com.memeboo2.haemi.auth.account.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.verification.application.PhoneVerificationUseCase;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterGuardianUseCase {

    private final AccountRepository accountRepository;
    private final PasswordService passwordService;
    private final PhoneVerificationUseCase phoneVerificationUseCase;

    @Transactional
    public UUID execute(String name, String loginId, String password,
                        String birthDate, String phone, String pin, UUID phoneVerificationId) {
        if (accountRepository.existsByLoginId(loginId)) {
            throw new DomainException(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
        }
        phoneVerificationUseCase.consumeVerified(phoneVerificationId, phone);
        String hash = passwordService.encode(password);
        String pinHash = passwordService.encode(pin);
        Account account = Account.guardian(name, loginId, hash, birthDate, phone, pinHash);
        accountRepository.save(account);
        return account.getId();
    }
}
