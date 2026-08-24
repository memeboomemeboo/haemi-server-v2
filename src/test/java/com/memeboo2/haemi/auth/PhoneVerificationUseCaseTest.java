package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.verification.application.PhoneVerificationProperties;
import com.memeboo2.haemi.auth.verification.application.PhoneVerificationUseCase;
import com.memeboo2.haemi.auth.verification.application.SmsSender;
import com.memeboo2.haemi.auth.verification.application.VerificationCodeGenerator;
import com.memeboo2.haemi.auth.verification.domain.PhoneVerification;
import com.memeboo2.haemi.auth.verification.infrastructure.PhoneVerificationRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-08-23T00:00:00Z");
    private static final String PHONE = "01012345678";

    @Mock PhoneVerificationRepository repository;
    @Mock PasswordService passwordService;
    @Mock VerificationCodeGenerator codeGenerator;
    @Mock SmsSender smsSender;
    @Mock HaemiClock clock;
    @Mock PhoneVerificationProperties properties;
    @InjectMocks PhoneVerificationUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(clock.now()).thenReturn(NOW);
        lenient().when(properties.maxConfirmAttempts()).thenReturn(5);
        lenient().when(properties.maxResendPerWindow()).thenReturn(5);
        lenient().when(properties.resendWindowSeconds()).thenReturn(3600L);
    }

    @Test
    void 인증번호를_검증한_휴대폰_인증만_가입에서_소비할_수_있다() {
        UUID verificationId = UUID.randomUUID();
        PhoneVerification verification = PhoneVerification.pending(PHONE, "hashed-code", NOW.plusSeconds(300));
        given(repository.findById(verificationId)).willReturn(Optional.of(verification));
        given(passwordService.matches("123456", "hashed-code")).willReturn(true);

        useCase.confirm(verificationId, "123456");
        useCase.consumeVerified(verificationId, PHONE);

        assertThat(verification.getVerifiedAt()).isEqualTo(NOW);
        assertThat(verification.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void 다른_전화번호로는_검증된_인증을_소비할_수_없다() {
        UUID verificationId = UUID.randomUUID();
        PhoneVerification verification = PhoneVerification.pending(PHONE, "hashed-code", NOW.plusSeconds(300));
        given(repository.findById(verificationId)).willReturn(Optional.of(verification));
        given(passwordService.matches("123456", "hashed-code")).willReturn(true);
        useCase.confirm(verificationId, "123456");

        assertThatThrownBy(() -> useCase.consumeVerified(verificationId, "01099998888"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PHONE_VERIFICATION_REQUIRED));
    }

    @Test
    void 인증번호_확인을_상한만큼_틀리면_잠긴다() {
        UUID verificationId = UUID.randomUUID();
        PhoneVerification verification = PhoneVerification.pending(PHONE, "hashed-code", NOW.plusSeconds(300));
        given(repository.findById(verificationId)).willReturn(Optional.of(verification));
        given(passwordService.matches("000000", "hashed-code")).willReturn(false);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> useCase.confirm(verificationId, "000000"))
                    .isInstanceOf(DomainException.class)
                    .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
        }

        assertThatThrownBy(() -> useCase.confirm(verificationId, "123456"))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_VERIFICATION_LOCKED));
    }

    @Test
    void 재발송_제한을_초과하면_거부된다() {
        given(repository.countByPhoneAndCreatedAtAfter(PHONE, NOW.minusSeconds(3600))).willReturn(5L);

        assertThatThrownBy(() -> useCase.request(PHONE))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_VERIFICATION_RESEND_LIMITED));
    }

    @Test
    void 인증요청은_무작위_코드를_SMS로_전송한다() {
        UUID verificationId = UUID.randomUUID();
        given(codeGenerator.nextCode()).willReturn("123456");
        given(passwordService.encode("123456")).willReturn("hashed-code");
        given(repository.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> {
            PhoneVerification verification = invocation.getArgument(0);
            var idField = verification.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(verification, verificationId);
            return verification;
        });

        assertThat(useCase.request(PHONE)).isEqualTo(verificationId);
        verify(smsSender).sendVerificationCode(PHONE, "123456");
    }
}
