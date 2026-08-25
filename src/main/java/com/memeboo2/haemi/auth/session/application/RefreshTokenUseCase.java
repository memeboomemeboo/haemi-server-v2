package com.memeboo2.haemi.auth.session.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.auth.session.domain.RefreshToken;
import com.memeboo2.haemi.auth.session.infrastructure.RefreshTokenRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * refresh 토큰으로 새 access 토큰을 발급한다.
 *
 * <p>저장된 토큰과 대조하고 만료·기기 일치를 검증한 뒤, 재발급 시 refresh 토큰을 회전(rotation)한다.
 * 회전하지 않으면 유출된 refresh 토큰이 만료까지 계속 유효하므로, 로그인과 동일하게 기기별 1개만 유지한다.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final HaemiClock clock;

    @Transactional
    public LoginUseCase.TokenPair execute(String refreshToken, String deviceId) {
        // 서명·형식 검증 (위조·손상 토큰 조기 거부)
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw new DomainException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new DomainException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));

        // 다른 기기의 토큰으로는 재발급하지 않는다
        if (!stored.getDeviceId().equals(deviceId)) {
            throw new DomainException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new DomainException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        Account account = accountRepository.findById(stored.getAccountId())
                .orElseThrow(() -> new DomainException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));

        String newAccessToken = jwtTokenProvider.createAccessToken(account.getId(), account.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(account.getId());

        // 토큰 회전: 기기별 기존 토큰을 제거하고 새 토큰만 유지
        Instant refreshExpiry = clock.now().plus(jwtProperties.refreshTokenValidity());
        refreshTokenRepository.deleteByAccountIdAndDeviceId(account.getId(), deviceId);
        refreshTokenRepository.save(RefreshToken.of(account.getId(), deviceId, newRefreshToken, refreshExpiry));

        return new LoginUseCase.TokenPair(newAccessToken, newRefreshToken);
    }
}
