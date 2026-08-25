package com.memeboo2.haemi.auth.verification.infrastructure;

import com.memeboo2.haemi.auth.verification.domain.VerificationRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface VerificationRateLimitRepository extends JpaRepository<VerificationRateLimit, UUID> {

    /**
     * 이미 있는 윈도우 카운터를 원자적으로 1 올린다.
     * 읽어서 +1 하는 방식은 동시 요청이 서로의 증가분을 덮어써 제한이 누적되지 않는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE VerificationRateLimit r
               SET r.attemptCount = r.attemptCount + 1
             WHERE r.rateKey = :rateKey AND r.windowStart = :windowStart
            """)
    int incrementIfPresent(@Param("rateKey") String rateKey, @Param("windowStart") Instant windowStart);

    @Query("SELECT r.attemptCount FROM VerificationRateLimit r WHERE r.rateKey = :rateKey AND r.windowStart = :windowStart")
    Integer findAttemptCount(@Param("rateKey") String rateKey, @Param("windowStart") Instant windowStart);
}
