package com.memeboo2.haemi.auth.verification;

import com.memeboo2.haemi.auth.verification.domain.VerificationRateLimit;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationRateLimitDomainTest {

    @Test
    void firstAttempt로_카운터를_생성하면_시도횟수는_1이다() {
        Instant windowStart = Instant.parse("2025-01-01T00:00:00Z");

        VerificationRateLimit limit = VerificationRateLimit.firstAttempt("test@example.com", windowStart);

        assertThat(limit.getRateKey()).isEqualTo("test@example.com");
        assertThat(limit.getWindowStart()).isEqualTo(windowStart);
        assertThat(limit.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void 다른_키로_생성하면_별도_카운터이다() {
        Instant now = Instant.now();

        VerificationRateLimit l1 = VerificationRateLimit.firstAttempt("a@test.com", now);
        VerificationRateLimit l2 = VerificationRateLimit.firstAttempt("b@test.com", now);

        assertThat(l1.getRateKey()).isNotEqualTo(l2.getRateKey());
        assertThat(l1.getAttemptCount()).isEqualTo(1);
        assertThat(l2.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void 같은_키_다른_윈도우로_생성할_수_있다() {
        String key = "user@test.com";
        Instant w1 = Instant.parse("2025-01-01T00:00:00Z");
        Instant w2 = Instant.parse("2025-01-01T01:00:00Z");

        VerificationRateLimit l1 = VerificationRateLimit.firstAttempt(key, w1);
        VerificationRateLimit l2 = VerificationRateLimit.firstAttempt(key, w2);

        assertThat(l1.getWindowStart()).isNotEqualTo(l2.getWindowStart());
    }
}
