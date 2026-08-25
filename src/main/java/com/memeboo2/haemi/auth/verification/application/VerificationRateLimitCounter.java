package com.memeboo2.haemi.auth.verification.application;

import com.memeboo2.haemi.auth.verification.infrastructure.VerificationRateLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 발송 제한용 고정 윈도우 카운터를 올리고 증가 후 값을 돌려준다.
 *
 * <p>DB 벤더 전용 upsert(ON CONFLICT) 대신 표준 JPQL만 쓴다 — 운영은 Postgres지만
 * 테스트·로컬은 H2라 벤더 전용 구문은 그 환경에서 그대로 500이 된다.
 * 증가는 원자적 UPDATE, 첫 행 생성은 별도 트랜잭션에서 시도하고 unique 충돌은 재증가로 흡수한다.
 *
 * <p>요청이 다른 이유로 롤백돼도 카운터는 남도록 REQUIRES_NEW로 커밋한다.
 */
@Component
@RequiredArgsConstructor
public class VerificationRateLimitCounter {

    private final VerificationRateLimitRepository repository;
    private final VerificationRateLimitCreator creator;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int incrementAndGet(String rateKey, Instant windowStart) {
        if (repository.incrementIfPresent(rateKey, windowStart) == 0) {
            try {
                creator.createFirstAttempt(rateKey, windowStart);
                return 1;
            } catch (RateLimitRowExistsException createdByAnotherRequest) {
                repository.incrementIfPresent(rateKey, windowStart);
            }
        }
        Integer attemptCount = repository.findAttemptCount(rateKey, windowStart);
        return attemptCount == null ? 1 : attemptCount;
    }
}
