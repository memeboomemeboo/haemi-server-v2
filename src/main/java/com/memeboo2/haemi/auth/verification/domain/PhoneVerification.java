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
@Table(name = "phone_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    public static PhoneVerification pending(String phone, String codeHash, Instant expiresAt) {
        PhoneVerification verification = new PhoneVerification();
        verification.phone = phone;
        verification.codeHash = codeHash;
        verification.expiresAt = expiresAt;
        return verification;
    }

    public void markVerified(Instant now) {
        requireNotExpired(now);
        if (consumedAt != null) {
            throw new DomainException(ErrorCode.PHONE_VERIFICATION_REQUIRED, "이미 사용된 휴대폰 인증입니다.");
        }
        verifiedAt = now;
    }

    public boolean isLocked(int maxAttempts) {
        return failCount >= maxAttempts;
    }

    public void recordFailedAttempt() {
        failCount++;
    }

    public void consumeFor(String requestedPhone, Instant now) {
        requireNotExpired(now);
        if (!phone.equals(requestedPhone) || verifiedAt == null || consumedAt != null) {
            throw new DomainException(ErrorCode.PHONE_VERIFICATION_REQUIRED, "휴대폰 인증이 필요합니다.");
        }
        consumedAt = now;
    }

    private void requireNotExpired(Instant now) {
        if (!now.isBefore(expiresAt)) {
            throw new DomainException(ErrorCode.PHONE_VERIFICATION_REQUIRED, "휴대폰 인증 시간이 만료되었습니다.");
        }
    }
}
