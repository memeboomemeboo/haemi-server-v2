package com.memeboo2.haemi.auth.session.application;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.session.domain.RefreshToken;
import com.memeboo2.haemi.auth.session.infrastructure.RefreshTokenRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
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

    public record TokenPair(String accessToken, String refreshToken) {}

    @Transactional
    public TokenPair execute(String loginId, String password) {
        Account account = accountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new DomainException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordService.matches(password, account.getPasswordHash())) {
            throw new DomainException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(account.getId(), account.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(account.getId());

        Instant refreshExpiry = Instant.now().plus(jwtProperties.refreshTokenValidity());
        refreshTokenRepository.deleteByAccountId(account.getId());
        refreshTokenRepository.save(RefreshToken.of(account.getId(), refreshToken, refreshExpiry));

        return new TokenPair(accessToken, refreshToken);
    }
}
