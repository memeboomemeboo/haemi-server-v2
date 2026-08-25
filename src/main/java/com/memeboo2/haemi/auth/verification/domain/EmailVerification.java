package com.memeboo2.haemi.auth.verification.domain;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "email_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    /** 실패 카운터는 원자적 UPDATE로만 증가시킨다 (EmailVerificationRepository#incrementFailCount). */
    @Column(name = "fail_count", nullable = false)
    private int failCount;

    public static EmailVerification pending(String email, String codeHash, Instant expiresAt) {
        EmailVerification verification = new EmailVerification();
        verification.email = email;
        verification.codeHash = codeHash;
        verification.expiresAt = expiresAt;
        return verification;
    }

    public void markVerified(Instant now) {
        requireNotExpired(now);
        if (consumedAt != null) {
            throw new DomainException(ErrorCode.EMAIL_VERIFICATION_REQUIRED, "이미 사용된 이메일 인증입니다.");
        }
        verifiedAt = now;
    }

    public boolean isLocked(int maxAttempts) {
        return failCount >= maxAttempts;
    }

    public void consumeFor(String requestedEmail, Instant now) {
        requireNotExpired(now);
        if (!email.equals(requestedEmail) || verifiedAt == null || consumedAt != null) {
            throw new DomainException(ErrorCode.EMAIL_VERIFICATION_REQUIRED, "이메일 인증이 필요합니다.");
        }
        consumedAt = now;
    }

    private void requireNotExpired(Instant now) {
        if (!now.isBefore(expiresAt)) {
            throw new DomainException(ErrorCode.EMAIL_VERIFICATION_REQUIRED, "이메일 인증 시간이 만료되었습니다.");
        }
    }
}
