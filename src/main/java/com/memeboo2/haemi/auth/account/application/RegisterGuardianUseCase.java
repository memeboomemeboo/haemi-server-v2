package com.memeboo2.haemi.auth.account.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
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
public class RegisterGuardianUseCase {

    private final AccountRepository accountRepository;
    private final PasswordService passwordService;

    // 확정 디자인(#100 X1): 회원가입은 이름·생년월일·아이디·비밀번호 + PIN 만 수집한다.
    // 이메일 인증/전화번호는 디자인에 없어 제거했다(이메일 인증 플로우 자체는 §4 보류로 잔존).
    @Transactional
    public UUID execute(String name, String loginId, String password, String birthDate, String pin) {
        if (accountRepository.existsByLoginId(loginId)) {
            throw new DomainException(ErrorCode.LOGIN_ID_ALREADY_TAKEN);
        }
        String hash = passwordService.encode(password);
        String pinHash = passwordService.encode(pin);
        Account account = Account.guardian(name, loginId, hash, birthDate, null, null, pinHash);
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
