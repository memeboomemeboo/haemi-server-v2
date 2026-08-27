package com.memeboo2.haemi.auth.verification;

import com.memeboo2.haemi.auth.verification.domain.EmailVerification;
import com.memeboo2.haemi.common.error.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** EmailVerification의 생성/검증/소비 흐름을 검증한다. */
class EmailVerificationDomainTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-08-27T00:10:00Z");
    private static final Instant BEFORE_EXPIRY = Instant.parse("2026-08-27T00:05:00Z");
    private static final Instant AFTER_EXPIRY = Instant.parse("2026-08-27T00:20:00Z");

    @Test
    void pending은_대기_상태의_인증을_생성한다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);

        assertThat(verification.getEmail()).isEqualTo("test@test.com");
        assertThat(verification.getCodeHash()).isEqualTo("code-hash");
        assertThat(verification.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(verification.getVerifiedAt()).isNull();
        assertThat(verification.getConsumedAt()).isNull();
        assertThat(verification.getFailCount()).isEqualTo(0);
    }

    @Test
    void markVerified는_만료_전이면_인증_시각을_기록한다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);

        verification.markVerified(BEFORE_EXPIRY);

        assertThat(verification.getVerifiedAt()).isEqualTo(BEFORE_EXPIRY);
    }

    @Test
    void markVerified는_만료_시각_이후면_예외가_발생한다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);

        assertThatThrownBy(() -> verification.markVerified(AFTER_EXPIRY))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void markVerified는_만료_시각과_정확히_같으면_예외가_발생한다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);

        assertThatThrownBy(() -> verification.markVerified(EXPIRES_AT))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void markVerified는_이미_소비된_인증이면_예외가_발생한다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);
        verification.markVerified(BEFORE_EXPIRY);
        verification.consumeFor("test@test.com", BEFORE_EXPIRY);

        assertThatThrownBy(() -> verification.markVerified(BEFORE_EXPIRY))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void isLocked는_실패_횟수가_최대치_이상이면_true다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);

        assertThat(verification.isLocked(5)).isFalse();
    }

    @Test
    void isLocked는_실패_횟수가_0이고_최대치가_0이면_true다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);

        assertThat(verification.isLocked(0)).isTrue();
    }

    @Test
    void consumeFor는_이메일이_일치하고_인증되었으면_소비된다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);
        verification.markVerified(BEFORE_EXPIRY);

        verification.consumeFor("test@test.com", BEFORE_EXPIRY);

        assertThat(verification.getConsumedAt()).isEqualTo(BEFORE_EXPIRY);
    }

    @Test
    void consumeFor는_이메일이_다르면_예외가_발생한다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);
        verification.markVerified(BEFORE_EXPIRY);

        assertThatThrownBy(() -> verification.consumeFor("other@test.com", BEFORE_EXPIRY))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void consumeFor는_아직_인증되지_않았으면_예외가_발생한다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);

        assertThatThrownBy(() -> verification.consumeFor("test@test.com", BEFORE_EXPIRY))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void consumeFor는_이미_소비되었으면_예외가_발생한다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);
        verification.markVerified(BEFORE_EXPIRY);
        verification.consumeFor("test@test.com", BEFORE_EXPIRY);

        assertThatThrownBy(() -> verification.consumeFor("test@test.com", BEFORE_EXPIRY))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void consumeFor는_만료된_이후면_예외가_발생한다() {
        EmailVerification verification = EmailVerification.pending("test@test.com", "code-hash", EXPIRES_AT);
        verification.markVerified(BEFORE_EXPIRY);

        assertThatThrownBy(() -> verification.consumeFor("test@test.com", AFTER_EXPIRY))
                .isInstanceOf(DomainException.class);
    }
}
