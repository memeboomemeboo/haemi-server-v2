package com.memeboo2.haemi.auth.session.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** loginId 없이 입력된 PIN을 어르신 계정의 BCrypt PIN 해시와만 대조하는 전용 로그인 흐름. */
@Service
@RequiredArgsConstructor
public class ElderPinLoginUseCase {

    private final AccountRepository accountRepository;
    private final PasswordService passwordService;
    private final JwtTokenProvider jwtTokenProvider;
    private final HaemiClock clock;

    @Transactional
    public String execute(String pin) {
        List<Account> matchedAccounts = accountRepository.findAllByRole(AccountRole.ELDER).stream()
                .filter(account -> account.getPinHash() != null && passwordService.matches(pin, account.getPinHash()))
                .toList();

        // PIN만으로 특정 계정을 골라야 하므로, 중복 PIN은 어느 계정도 인증하지 않는다.
        if (matchedAccounts.size() != 1) {
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS);
        }

        Account account = matchedAccounts.getFirst();
        Instant now = clock.now();
        if (account.isLocked(now) || accountRepository.recordLoginSuccess(account.getId(), now) == 0) {
            throw new DomainException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        return jwtTokenProvider.createAccessToken(account.getId(), AccountRole.ELDER);
    }
}
