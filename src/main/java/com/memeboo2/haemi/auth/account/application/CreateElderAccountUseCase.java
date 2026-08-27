package com.memeboo2.haemi.auth.account.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.AccountCommand;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateElderAccountUseCase implements AccountCommand {

    private final AccountRepository accountRepository;
    private final PasswordService passwordService;

    @Override
    @Transactional
    public UUID createElderAccount(String name, String loginId, String credential,
                                   String birthDate, String phone, String gender) {
        if (accountRepository.existsByLoginId(loginId)) {
            throw new DomainException(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
        }
        String credentialHash = passwordService.encode(credential);
        Account account = Account.elder(name, loginId, credentialHash, birthDate, phone, gender);
        accountRepository.save(account);
        return account.getId();
    }
}
