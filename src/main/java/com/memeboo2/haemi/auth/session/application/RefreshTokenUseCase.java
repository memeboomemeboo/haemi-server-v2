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
    private final RefreshTokenMaintenance refreshTokenMaintenance;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final HaemiClock clock;

    @Transactional
    public LoginUseCase.TokenPair execute(String refreshToken, String deviceId) {
        // 서명·형식 검증 (위조·손상 토큰 조기 거부)
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw new DomainException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        // 기기까지 일치하는 토큰만 조회한다 (다른 기기 토큰으로는 재발급하지 않음).
        RefreshToken stored = refreshTokenRepository.findByTokenAndDeviceId(refreshToken, deviceId)
                .orElseThrow(() -> new DomainException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));

        if (stored.isExpired(clock.now())) {
            // 별도 트랜잭션으로 정리해, 아래 예외 롤백에 삭제가 휩쓸리지 않게 한다.
            refreshTokenMaintenance.purge(refreshToken, deviceId);
            throw new DomainException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        Account account = accountRepository.findById(stored.getAccountId())
                .orElseThrow(() -> new DomainException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));

        if (account.isLocked(clock.now())) {
            throw new DomainException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        // 단일 소비(회전): 이 토큰 행을 조건부로 제거하고, 실제로 지운 요청만 재발급을 진행한다.
        // 동일 토큰으로 동시 요청이 들어와도 DB가 삭제를 직렬화해 한 요청만 성공한다 (재사용·이중 발급 차단).
        int consumed = refreshTokenRepository.deleteByTokenAndDeviceId(refreshToken, deviceId);
        if (consumed == 0) {
            throw new DomainException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(account.getId(), account.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(account.getId());

        Instant refreshExpiry = clock.now().plus(jwtProperties.refreshTokenValidity());
        refreshTokenRepository.save(RefreshToken.of(account.getId(), deviceId, newRefreshToken, refreshExpiry));

        return new LoginUseCase.TokenPair(newAccessToken, newRefreshToken);
    }
}
