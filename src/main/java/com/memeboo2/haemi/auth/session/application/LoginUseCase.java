package com.memeboo2.haemi.auth.session.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.session.domain.RefreshToken;
import com.memeboo2.haemi.auth.session.infrastructure.RefreshTokenRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordService passwordService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final LoginProperties loginProperties;
    private final HaemiClock clock;

    public record TokenPair(String accessToken, String refreshToken) {}

    @Transactional
    public TokenPair execute(String loginId, String password, String pin, String deviceId) {
        Account account = accountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new DomainException(ErrorCode.INVALID_CREDENTIALS));

        Instant now = clock.now();
        if (account.isLocked(now)) {
            throw new DomainException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        boolean passwordMatches = password != null && !password.isBlank()
                && passwordService.matches(password, account.getPasswordHash());
        boolean pinMatches = pin != null && !pin.isBlank()
                && account.isPinLoginEnabled()
                && account.getPinHash() != null
                && passwordService.matches(pin, account.getPinHash());
        if (!passwordMatches && !pinMatches) {
            account.recordLoginFailure(now, loginProperties.maxFailedAttempts(), loginProperties.lockDurationSeconds());
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS);
        }
        account.recordLoginSuccess();
        if (passwordMatches && !account.isPinLoginEnabled()) {
            account.enablePinLogin();
        }

        String accessToken = jwtTokenProvider.createAccessToken(account.getId(), account.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(account.getId());

        Instant refreshExpiry = now.plus(jwtProperties.refreshTokenValidity());
        refreshTokenRepository.deleteByAccountIdAndDeviceId(account.getId(), deviceId);
        refreshTokenRepository.save(RefreshToken.of(account.getId(), deviceId, refreshToken, refreshExpiry));

        return new TokenPair(accessToken, refreshToken);
    }
}
