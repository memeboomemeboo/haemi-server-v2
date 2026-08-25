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
     * 고정 윈도우 카운터를 원자적으로 1 올린다.
     * count-then-insert(또는 프로세스 로컬 락)는 다중 인스턴스에서 공유되지 않아
     * 인스턴스 수만큼 제한이 늘어난다. upsert 한 번으로 DB에서 직렬화한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO auth_verification_rate_limits (rate_key, window_start, attempt_count)
            VALUES (:rateKey, :windowStart, 1)
            ON CONFLICT (rate_key, window_start)
            DO UPDATE SET attempt_count = auth_verification_rate_limits.attempt_count + 1,
                          updated_at = now()
            """, nativeQuery = true)
    void increment(@Param("rateKey") String rateKey, @Param("windowStart") Instant windowStart);

    /**
     * 증가 직후 값을 읽는다. upsert가 해당 행을 커밋 시점까지 잠그므로
     * 같은 트랜잭션에서 읽은 값은 다른 요청의 증가분과 섞이지 않는다.
     */
    @Query("SELECT r.attemptCount FROM VerificationRateLimit r WHERE r.rateKey = :rateKey AND r.windowStart = :windowStart")
    Integer findAttemptCount(@Param("rateKey") String rateKey, @Param("windowStart") Instant windowStart);
}
