package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.verification.application.EmailSender;
import com.memeboo2.haemi.auth.verification.application.EmailVerificationProperties;
import com.memeboo2.haemi.auth.verification.application.EmailVerificationUseCase;
import com.memeboo2.haemi.auth.verification.application.VerificationCodeGenerator;
import com.memeboo2.haemi.auth.verification.application.VerificationFailureRecorder;
import com.memeboo2.haemi.auth.verification.domain.EmailVerification;
import com.memeboo2.haemi.auth.verification.infrastructure.EmailVerificationRepository;
import com.memeboo2.haemi.auth.verification.infrastructure.VerificationRateLimitRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailVerificationUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-08-23T00:00:00Z");
    private static final String EMAIL = "guardian@example.com";

    @Mock EmailVerificationRepository repository;
    @Mock VerificationRateLimitRepository rateLimitRepository;
    @Mock PasswordService passwordService;
    @Mock VerificationCodeGenerator codeGenerator;
    @Mock EmailSender emailSender;
    @Mock HaemiClock clock;
    @Mock EmailVerificationProperties properties;
    @Mock VerificationFailureRecorder verificationFailureRecorder;
    @InjectMocks EmailVerificationUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(clock.now()).thenReturn(NOW);
        lenient().when(properties.maxConfirmAttempts()).thenReturn(5);
        lenient().when(properties.maxResendPerWindow()).thenReturn(5);
        lenient().when(properties.resendWindowSeconds()).thenReturn(3600L);
    }

    @Test
    void 인증번호를_검증한_이메일_인증만_가입에서_소비할_수_있다() {
        UUID verificationId = UUID.randomUUID();
        EmailVerification verification = EmailVerification.pending(EMAIL, "hashed-code", NOW.plusSeconds(300));
        given(repository.findById(verificationId)).willReturn(Optional.of(verification));
        given(passwordService.matches("123456", "hashed-code")).willReturn(true);

        useCase.confirm(verificationId, "123456");
        useCase.consumeVerified(verificationId, EMAIL);

        assertThat(verification.getVerifiedAt()).isEqualTo(NOW);
        assertThat(verification.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void 대소문자만_다른_이메일도_같은_인증으로_소비된다() {
        UUID verificationId = UUID.randomUUID();
        EmailVerification verification = EmailVerification.pending(EMAIL, "hashed-code", NOW.plusSeconds(300));
        given(repository.findById(verificationId)).willReturn(Optional.of(verification));
        given(passwordService.matches("123456", "hashed-code")).willReturn(true);
        useCase.confirm(verificationId, "123456");

        useCase.consumeVerified(verificationId, "Guardian@Example.com");

        assertThat(verification.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void 다른_이메일로는_검증된_인증을_소비할_수_없다() {
        UUID verificationId = UUID.randomUUID();
        EmailVerification verification = EmailVerification.pending(EMAIL, "hashed-code", NOW.plusSeconds(300));
        given(repository.findById(verificationId)).willReturn(Optional.of(verification));
        given(passwordService.matches("123456", "hashed-code")).willReturn(true);
        useCase.confirm(verificationId, "123456");

        assertThatThrownBy(() -> useCase.consumeVerified(verificationId, "other@example.com"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_VERIFICATION_REQUIRED));
    }

    @Test
    void 인증번호_확인을_상한만큼_틀리면_잠긴다() {
        UUID verificationId = UUID.randomUUID();
        EmailVerification verification = EmailVerification.pending(EMAIL, "hashed-code", NOW.plusSeconds(300));
        given(repository.findById(verificationId)).willReturn(Optional.of(verification));
        given(passwordService.matches("000000", "hashed-code")).willReturn(false);
        // 실패 카운터는 별도 트랜잭션에서 원자적으로 증가하고 증가 후 값을 돌려준다.
        given(verificationFailureRecorder.recordFailure(verificationId)).willReturn(1, 2, 3, 4, 5);

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> useCase.confirm(verificationId, "000000"))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
        }

        assertThatThrownBy(() -> useCase.confirm(verificationId, "000000"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_VERIFICATION_LOCKED));
    }

    @Test
    void 재발송_제한을_초과하면_거부된다() {
        given(rateLimitRepository.findAttemptCount(anyString(), any())).willReturn(6);

        assertThatThrownBy(() -> useCase.request(EMAIL))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_VERIFICATION_RESEND_LIMITED));
    }

    @Test
    void 발송_제한은_고정_윈도우_카운터를_원자적으로_증가시켜_판정한다() {
        given(rateLimitRepository.findAttemptCount(anyString(), any())).willReturn(1);
        given(codeGenerator.nextCode()).willReturn("123456");
        given(passwordService.encode("123456")).willReturn("hashed-code");
        UUID verificationId = UUID.randomUUID();
        given(repository.save(any())).willAnswer(invocation -> {
            EmailVerification verification = invocation.getArgument(0);
            var idField = verification.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(verification, verificationId);
            return verification;
        });

        assertThat(useCase.request(EMAIL)).isEqualTo(verificationId);
        verify(emailSender).sendVerificationCode(EMAIL, "123456");
        // 윈도우 시작 시각은 resendWindowSeconds 단위로 내림한 값이어야 하고,
        // 키는 rate_key 컬럼(255자)을 넘지 않도록 해시된 고정 길이여야 한다.
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimitRepository).increment(keyCaptor.capture(), eq(Instant.parse("2026-08-23T00:00:00Z")));
        assertThat(keyCaptor.getValue()).startsWith("email-verification:").hasSizeLessThan(255);
    }
}
