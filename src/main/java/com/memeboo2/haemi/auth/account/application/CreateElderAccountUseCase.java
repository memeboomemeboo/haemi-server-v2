package com.memeboo2.haemi.auth.account.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.AccountCommand;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.persistence.ConstraintViolations;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateElderAccountUseCase implements AccountCommand {

    private final AccountRepository accountRepository;
    private final PasswordService passwordService;

    /** 단일 크리덴셜 호출부는 이를 PIN과 보조 비밀번호 양쪽에 사용한다. */
    public UUID createElderAccount(String name, String loginId, String credential,
                                   String birthDate, String phone, String gender) {
        return createElderAccount(name, loginId, credential, null, birthDate, phone, gender);
    }

    @Override
    @Transactional
    public UUID createElderAccount(String name, String loginId, String pin, String password,
                                   String birthDate, String phone, String gender) {
        if (accountRepository.existsByLoginId(loginId)) {
            throw new DomainException(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
        }
        String pinHash = passwordService.encode(pin);
        String passwordHash = password == null ? pinHash : passwordService.encode(password);
        Account account = Account.elder(name, loginId, passwordHash, pinHash, birthDate, phone, gender);
        try {
            // 중복확인 API는 예약을 하지 않으므로, 선검사와 INSERT 사이 경합도 409로 일관되게 변환한다.
            accountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.isViolationOf(e, "uk_accounts_login_id")) {
                throw new DomainException(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
            }
            throw e;
        }
        return account.getId();
    }
}
