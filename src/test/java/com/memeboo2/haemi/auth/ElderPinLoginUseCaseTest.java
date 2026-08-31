package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.session.application.ElderPinLoginUseCase;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ElderPinLoginUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-08-31T00:00:00Z");

    @Mock AccountRepository accountRepository;
    @Mock PasswordService passwordService;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock HaemiClock clock;
    @InjectMocks ElderPinLoginUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(clock.now()).thenReturn(NOW);
        lenient().when(accountRepository.recordLoginSuccess(any(), any())).thenReturn(1);
    }

    @Test
    void 어르신은_PIN만으로_액세스토큰을_발급받는다() {
        UUID elderId = UUID.randomUUID();
        Account elder = elder(elderId, "pin-hash");
        given(accountRepository.findAllByRole(AccountRole.ELDER)).willReturn(List.of(elder));
        given(passwordService.matches("123456", "pin-hash")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(elderId, AccountRole.ELDER)).willReturn("elder-access-token");

        String accessToken = useCase.execute("123456");

        assertThat(accessToken).isEqualTo("elder-access-token");
    }

    @Test
    void 일치하는_PIN이_없으면_인증에_실패한다() {
        Account elder = elder(UUID.randomUUID(), "pin-hash");
        given(accountRepository.findAllByRole(AccountRole.ELDER)).willReturn(List.of(elder));
        given(passwordService.matches("000000", "pin-hash")).willReturn(false);

        assertThatThrownBy(() -> useCase.execute("000000"))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 같은_PIN을_쓰는_어르신이_둘이면_인증하지_않는다() {
        Account first = elder(UUID.randomUUID(), "first-pin-hash");
        Account second = elder(UUID.randomUUID(), "second-pin-hash");
        given(accountRepository.findAllByRole(AccountRole.ELDER)).willReturn(List.of(first, second));
        given(passwordService.matches("123456", "first-pin-hash")).willReturn(true);
        given(passwordService.matches("123456", "second-pin-hash")).willReturn(true);

        assertThatThrownBy(() -> useCase.execute("123456"))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    private static Account elder(UUID id, String pinHash) {
        Account account = Account.elder("어르신", "elder" + id, "password-hash", pinHash,
                "1945-01-01", null, null);
        try {
            var field = account.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
            return account;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
