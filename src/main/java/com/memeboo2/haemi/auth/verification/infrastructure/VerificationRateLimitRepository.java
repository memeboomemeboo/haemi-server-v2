package com.memeboo2.haemi.auth.verification.infrastructure;

import com.memeboo2.haemi.auth.verification.domain.VerificationRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface VerificationRateLimitRepository extends JpaRepository<VerificationRateLimit, UUID> {

    /**
     * 고정 윈도우 카운터를 원자적으로 1 올리고 증가 후 값을 돌려준다.
     * count-then-insert(또는 프로세스 로컬 락)는 다중 인스턴스에서 공유되지 않아
     * 인스턴스 수만큼 제한이 늘어난다. upsert 한 번으로 DB에서 직렬화한다.
     */
    @Query(value = """
            INSERT INTO auth_verification_rate_limits (rate_key, window_start, attempt_count)
            VALUES (:rateKey, :windowStart, 1)
            ON CONFLICT (rate_key, window_start)
            DO UPDATE SET attempt_count = auth_verification_rate_limits.attempt_count + 1,
                          updated_at = now()
            RETURNING attempt_count
            """, nativeQuery = true)
    int incrementAndGet(@Param("rateKey") String rateKey, @Param("windowStart") Instant windowStart);
}
