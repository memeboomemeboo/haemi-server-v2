package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.session.application.JwtProperties;
import com.memeboo2.haemi.auth.session.application.LoginUseCase;
import com.memeboo2.haemi.auth.session.infrastructure.RefreshTokenRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock AccountRepository accountRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordService passwordService;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock JwtProperties jwtProperties;
    @InjectMocks LoginUseCase useCase;

    @Test
    void 첫_로그인은_PIN만으로_할_수_없다() {
        Account account = guardian();
        given(accountRepository.findByLoginId("guardian01")).willReturn(Optional.of(account));

        assertThatThrownBy(() -> useCase.execute("guardian01", null, "123456", "device-a"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void 비밀번호_첫_로그인_후에는_PIN_로그인이_허용된다() {
        Account account = guardian();
        UUID accountId = UUID.randomUUID();
        setId(account, accountId);
        given(accountRepository.findByLoginId("guardian01")).willReturn(Optional.of(account));
        given(passwordService.matches("password1", "password-hash")).willReturn(true);
        given(passwordService.matches("123456", "pin-hash")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(accountId, AccountRole.GUARDIAN)).willReturn("access");
        given(jwtTokenProvider.createRefreshToken(accountId)).willReturn("refresh");
        given(jwtProperties.refreshTokenValidity()).willReturn(Duration.ofDays(14));

        useCase.execute("guardian01", "password1", null, "device-a");
        LoginUseCase.TokenPair pinLogin = useCase.execute("guardian01", null, "123456", "device-a");

        assertThat(account.isPinLoginEnabled()).isTrue();
        assertThat(pinLogin.accessToken()).isEqualTo("access");
    }

    private static Account guardian() {
        return Account.guardian("보호자", "guardian01", "password-hash", "1970-01-01", "01012345678", "pin-hash");
    }

    private static void setId(Account account, UUID id) {
        try {
            var idField = account.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(account, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
