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
    private final LoginFailureRecorder loginFailureRecorder;

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
            // PIN(6자리 = 100만 조합)은 비밀번호보다 추측이 쉬워, PIN을 제출한 시도는
            // 더 낮은 임계값으로 잠근다. 실패 카운터(failed_login_attempts)는 공유하되
            // 이번 시도가 PIN이면 더 이른 시점에 잠금이 걸리도록 임계값만 낮춘다.
            boolean pinAttempt = pin != null && !pin.isBlank();
            int maxAttempts = pinAttempt
                    ? loginProperties.maxPinFailedAttempts()
                    : loginProperties.maxFailedAttempts();
            loginFailureRecorder.recordFailure(loginId, now,
                    maxAttempts, loginProperties.lockDurationSeconds());
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS);
        }
        // 성공 기록도 원자적 UPDATE로 한다. 엔티티를 수정해 flush하면 조회 이후 다른 요청이
        // 올린 실패 카운터·잠금을 stale 값으로 덮어써, 동시 요청에서 계정 잠금이 풀린다.
        if (accountRepository.recordLoginSuccess(account.getId(), now) == 0) {
            // 검증 도중 다른 실패 요청이 계정을 잠갔다.
            throw new DomainException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        if (passwordMatches && !account.isPinLoginEnabled()) {
            accountRepository.enablePinLogin(account.getId());
        }

        String accessToken = jwtTokenProvider.createAccessToken(account.getId(), account.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(account.getId());

        Instant refreshExpiry = now.plus(jwtProperties.refreshTokenValidity());
        refreshTokenRepository.deleteByAccountIdAndDeviceId(account.getId(), deviceId);
        refreshTokenRepository.save(RefreshToken.of(account.getId(), deviceId, refreshToken, refreshExpiry));

        return new TokenPair(accessToken, refreshToken);
    }
}
