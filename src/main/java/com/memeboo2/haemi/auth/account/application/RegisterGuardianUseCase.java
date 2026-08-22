package com.memeboo2.haemi.auth.account.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.credential.PasswordService;
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

    @Transactional
    public UUID execute(String name, String loginId, String password) {
        if (accountRepository.existsByLoginId(loginId)) {
            throw new DomainException(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
        }
        String hash = passwordService.encode(password);
        Account account = Account.guardian(name, loginId, hash);
        accountRepository.save(account);
        return account.getId();
    }
}
