package com.memeboo2.haemi.auth.account.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.verification.application.EmailVerificationUseCase;
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
public class RegisterGuardianUseCase {

    private final AccountRepository accountRepository;
    private final PasswordService passwordService;
    private final EmailVerificationUseCase emailVerificationUseCase;

    /** 선택 연락처가 없던 기존 호출부 호환용. */
    @Transactional
    public UUID execute(String name, String loginId, String password, String birthDate, String pin) {
        return execute(name, loginId, password, birthDate, pin, null, null, null);
    }

    /** 디자인 입력 외 이메일·전화는 선택 입력으로만 유지한다(design-api-spec B1). */
    @Transactional
    public UUID execute(String name, String loginId, String password, String birthDate, String pin,
                        String phone, String email, UUID emailVerificationId) {
        if (accountRepository.existsByLoginId(loginId)) {
            throw new DomainException(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
        }
        if (emailVerificationId != null && (email == null || email.isBlank())) {
            throw new DomainException(ErrorCode.INVALID_INPUT,
                    "이메일 인증번호를 사용하려면 이메일을 함께 입력해주세요.");
        }
        if (emailVerificationId != null) {
            emailVerificationUseCase.consumeVerified(emailVerificationId, email);
        }
        String hash = passwordService.encode(password);
        String pinHash = passwordService.encode(pin);
        Account account = Account.guardian(name, loginId, hash, birthDate, phone, email, pinHash);
        try {
            // 선검사(existsBy...)와 insert 사이에 다른 요청이 같은 loginId를 커밋할 수 있다.
            // 유니크 위반을 그대로 두면 커밋 시점에 500이 되므로, 여기서 409로 변환한다.
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
