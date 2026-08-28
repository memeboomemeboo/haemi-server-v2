package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.session.application.JwtProperties;
import com.memeboo2.haemi.auth.session.application.LoginFailureRecorder;
import com.memeboo2.haemi.auth.session.application.LoginProperties;
import com.memeboo2.haemi.auth.session.application.LoginUseCase;
import com.memeboo2.haemi.auth.session.infrastructure.RefreshTokenRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-08-25T00:00:00Z");

    @Mock AccountRepository accountRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordService passwordService;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock JwtProperties jwtProperties;
    @Mock LoginProperties loginProperties;
    @Mock HaemiClock clock;
    @Mock LoginFailureRecorder loginFailureRecorder;
    @InjectMocks LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        given(clock.now()).willReturn(NOW);
        lenient().when(loginProperties.maxFailedAttempts()).thenReturn(5);
        lenient().when(loginProperties.lockDurationSeconds()).thenReturn(900L);
        // 성공 기록은 원자적 UPDATE 한 건이다. 잠기지 않은 계정이면 1행이 갱신된다.
        lenient().when(accountRepository.recordLoginSuccess(any(), any())).thenReturn(1);
    }

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
        given(accountRepository.enablePinLogin(accountId)).willAnswer(invocation -> {
            account.enablePinLogin();
            return 1;
        });

        useCase.execute("guardian01", "password1", null, "device-a");
        LoginUseCase.TokenPair pinLogin = useCase.execute("guardian01", null, "123456", "device-a");

        assertThat(account.isPinLoginEnabled()).isTrue();
        assertThat(pinLogin.accessToken()).isEqualTo("access");
    }

    @Test
    void 로그인에_실패하면_설정값_그대로_실패_카운터를_기록한다() {
        Account account = guardian();
        given(accountRepository.findByLoginId("guardian01")).willReturn(Optional.of(account));
        given(passwordService.matches("wrong", "password-hash")).willReturn(false);

        assertThatThrownBy(() -> useCase.execute("guardian01", "wrong", null, "device-a"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        // 실제 증가·잠금은 별도 트랜잭션의 원자적 UPDATE가 담당한다.
        // 여기서는 잠금 임계값과 잠금 시간이 설정값 그대로 전달되는지만 확인한다.
        verify(loginFailureRecorder).recordFailure("guardian01", NOW, 5, 900L);
    }

    @Test
    void 검증_도중_다른_요청이_계정을_잠그면_성공_기록이_거부된다() {
        Account account = guardian();
        setId(account, UUID.randomUUID());
        given(accountRepository.findByLoginId("guardian01")).willReturn(Optional.of(account));
        given(passwordService.matches("password1", "password-hash")).willReturn(true);
        // 잠긴 계정에는 성공 UPDATE가 적용되지 않는다 (0행).
        given(accountRepository.recordLoginSuccess(any(), any())).willReturn(0);

        assertThatThrownBy(() -> useCase.execute("guardian01", "password1", null, "device-a"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_ACCOUNT_LOCKED));
    }

    @Test
    void PIN이_활성화된_계정은_PIN만으로_로그인된다() {
        Account account = guardian();
        UUID accountId = UUID.randomUUID();
        setId(account, accountId);
        account.enablePinLogin(); // 이미 PIN 로그인 활성화 상태
        given(accountRepository.findByLoginId("guardian01")).willReturn(Optional.of(account));
        // passwordMatches=false (비밀번호 미입력), pinMatches=true 경로
        given(passwordService.matches("123456", "pin-hash")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(accountId, AccountRole.GUARDIAN)).willReturn("access");
        given(jwtTokenProvider.createRefreshToken(accountId)).willReturn("refresh");
        given(jwtProperties.refreshTokenValidity()).willReturn(Duration.ofDays(14));

        LoginUseCase.TokenPair pair = useCase.execute("guardian01", null, "123456", "device-a");

        assertThat(pair.accessToken()).isEqualTo("access");
        // 이미 활성화되어 있으므로 enablePinLogin은 호출되지 않는다.
        verify(accountRepository, org.mockito.Mockito.never()).enablePinLogin(any());
    }

    @Test
    void 빈_비밀번호와_빈_PIN은_INVALID_CREDENTIALS() {
        Account account = guardian();
        given(accountRepository.findByLoginId("guardian01")).willReturn(Optional.of(account));

        // password="" → isBlank true 분기, pin="" → isBlank true 분기
        assertThatThrownBy(() -> useCase.execute("guardian01", "", "", "device-a"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void 잠긴_계정은_비밀번호가_맞아도_거부된다() {
        Account account = guardian();
        lockUntil(account, NOW.plusSeconds(900L));
        given(accountRepository.findByLoginId("guardian01")).willReturn(Optional.of(account));

        assertThatThrownBy(() -> useCase.execute("guardian01", "password1", null, "device-a"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_ACCOUNT_LOCKED));
    }

    private static Account guardian() {
        return Account.guardian("보호자", "guardian01", "password-hash", "1970-01-01", "01012345678", "guardian@example.com", "pin-hash");
    }

    /** 프로덕션 잠금은 원자적 UPDATE(AccountRepository)로만 일어나므로, 엔티티에는 잠금 세터가 없다.
     *  테스트에서 잠긴 상태를 재현하기 위해 필드를 직접 설정한다. */
    private static void lockUntil(Account account, Instant lockedUntil) {
        try {
            var field = Account.class.getDeclaredField("lockedUntil");
            field.setAccessible(true);
            field.set(account, lockedUntil);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
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
