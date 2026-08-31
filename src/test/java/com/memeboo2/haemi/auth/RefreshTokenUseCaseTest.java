package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.auth.session.application.JwtProperties;
import com.memeboo2.haemi.auth.session.application.LoginUseCase;
import com.memeboo2.haemi.auth.session.application.RefreshTokenUseCase;
import com.memeboo2.haemi.auth.session.domain.RefreshToken;
import com.memeboo2.haemi.auth.session.infrastructure.RefreshTokenRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Mock AccountRepository accountRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock com.memeboo2.haemi.auth.session.application.RefreshTokenMaintenance refreshTokenMaintenance;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock JwtProperties jwtProperties;
    @Mock HaemiClock clock;
    @InjectMocks RefreshTokenUseCase useCase;

    UUID accountId = UUID.randomUUID();
    String deviceId = "device-1";
    String refreshToken = "valid-refresh-token";

    private RefreshToken storedToken(String device, Instant expiresAt) {
        return RefreshToken.of(accountId, device, refreshToken, expiresAt);
    }

    private void stubHappyPath() {
        Account account = mock(Account.class);
        given(account.getId()).willReturn(accountId);
        given(account.getRole()).willReturn(AccountRole.GUARDIAN);

        given(clock.now()).willReturn(NOW);
        given(jwtProperties.refreshTokenValidity()).willReturn(Duration.ofDays(14));
        given(jwtTokenProvider.isValid(refreshToken)).willReturn(true);
        given(refreshTokenRepository.findByTokenAndDeviceId(refreshToken, deviceId))
                .willReturn(Optional.of(storedToken(deviceId, NOW.plus(Duration.ofDays(7)))));
        given(refreshTokenRepository.deleteByTokenAndDeviceId(refreshToken, deviceId)).willReturn(1);
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(jwtTokenProvider.createAccessToken(accountId, AccountRole.GUARDIAN)).willReturn("new-access");
        given(jwtTokenProvider.createRefreshToken(accountId)).willReturn("new-refresh");
    }

    @Test
    void 정상_재발급은_새_토큰을_발급하고_회전한다() {
        stubHappyPath();

        LoginUseCase.TokenPair pair = useCase.execute(refreshToken, deviceId);

        assertThat(pair.accessToken()).isEqualTo("new-access");
        assertThat(pair.refreshToken()).isEqualTo("new-refresh");
        // 회전: 이 토큰 행을 원자적으로 소비한 뒤 새 토큰 저장
        verify(refreshTokenRepository).deleteByTokenAndDeviceId(refreshToken, deviceId);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void 경쟁_요청이_이미_토큰을_소비했으면_401_이고_재발급하지_않는다() {
        Account account = mock(Account.class);
        lenient().when(account.getId()).thenReturn(accountId);
        given(clock.now()).willReturn(NOW);
        given(jwtTokenProvider.isValid(refreshToken)).willReturn(true);
        given(refreshTokenRepository.findByTokenAndDeviceId(refreshToken, deviceId))
                .willReturn(Optional.of(storedToken(deviceId, NOW.plus(Duration.ofDays(7)))));
        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        // 다른 요청이 먼저 소비 → 삭제 0건
        given(refreshTokenRepository.deleteByTokenAndDeviceId(refreshToken, deviceId)).willReturn(0);

        assertThatThrownBy(() -> useCase.execute(refreshToken, deviceId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        verify(refreshTokenRepository, org.mockito.Mockito.never()).save(any(RefreshToken.class));
    }

    @Test
    void 서명이_유효하지_않으면_401() {
        given(jwtTokenProvider.isValid(refreshToken)).willReturn(false);

        assertThatThrownBy(() -> useCase.execute(refreshToken, deviceId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
    }

    @Test
    void 저장되지_않은_토큰이면_401() {
        given(jwtTokenProvider.isValid(refreshToken)).willReturn(true);
        given(refreshTokenRepository.findByTokenAndDeviceId(refreshToken, deviceId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(refreshToken, deviceId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
    }

    @Test
    void 다른_기기의_토큰이면_401() {
        given(jwtTokenProvider.isValid(refreshToken)).willReturn(true);
        // 기기 포함 조회이므로 다른 기기의 토큰은 이 기기(deviceId)로 조회되지 않는다.
        given(refreshTokenRepository.findByTokenAndDeviceId(refreshToken, deviceId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(refreshToken, deviceId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
    }

    @Test
    void 만료된_토큰이면_삭제하고_401() {
        given(clock.now()).willReturn(NOW);
        given(jwtTokenProvider.isValid(refreshToken)).willReturn(true);
        RefreshToken expired = storedToken(deviceId, NOW.minus(Duration.ofDays(1)));
        given(refreshTokenRepository.findByTokenAndDeviceId(refreshToken, deviceId)).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> useCase.execute(refreshToken, deviceId))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        // 만료 정리는 별도 트랜잭션(REQUIRES_NEW)으로 커밋된다.
        verify(refreshTokenMaintenance).purge(refreshToken, deviceId);
    }
}
