package com.memeboo2.haemi.auth.account.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.verification.application.EmailVerificationUseCase;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterGuardianUseCase {

    private final AccountRepository accountRepository;
    private final PasswordService passwordService;
    private final EmailVerificationUseCase emailVerificationUseCase;

    @Transactional
    public UUID execute(String name, String loginId, String password,
                        String birthDate, String phone, String rawEmail, String pin, UUID emailVerificationId) {
        if (accountRepository.existsByLoginId(loginId)) {
            throw new DomainException(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (accountRepository.existsByEmail(email)) {
            throw new DomainException(ErrorCode.EMAIL_ALREADY_TAKEN);
        }
        emailVerificationUseCase.consumeVerified(emailVerificationId, email);
        String hash = passwordService.encode(password);
        String pinHash = passwordService.encode(pin);
        Account account = Account.guardian(name, loginId, hash, birthDate, phone, email, pinHash);
        accountRepository.save(account);
        return account.getId();
    }
}
