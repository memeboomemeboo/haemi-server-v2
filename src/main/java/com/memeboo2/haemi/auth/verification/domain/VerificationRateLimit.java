package com.memeboo2.haemi.auth.verification.domain;

import com.memeboo2.haemi.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** 인증번호 발송 제한용 고정 윈도우 카운터. 갱신은 원자적 upsert(네이티브)로만 수행한다. */
@Entity
@Table(name = "auth_verification_rate_limits",
        uniqueConstraints = @UniqueConstraint(name = "uk_verification_rate_limit",
                columnNames = {"rate_key", "window_start"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationRateLimit extends BaseEntity {

    @Column(name = "rate_key", nullable = false, length = 255)
    private String rateKey;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    public static VerificationRateLimit firstAttempt(String rateKey, Instant windowStart) {
        VerificationRateLimit rateLimit = new VerificationRateLimit();
        rateLimit.rateKey = rateKey;
        rateLimit.windowStart = windowStart;
        rateLimit.attemptCount = 1;
        return rateLimit;
    }
}
