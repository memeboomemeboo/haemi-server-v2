package com.memeboo2.haemi.auth.verification.application;

import com.memeboo2.haemi.auth.verification.domain.VerificationRateLimit;
import com.memeboo2.haemi.auth.verification.infrastructure.VerificationRateLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 윈도우 첫 요청의 카운터 행을 만든다.
 * 별도 트랜잭션인 이유: 동시에 같은 행을 만들면 unique 위반이 나는데,
 * 호출자의 트랜잭션에서 터지면 그 트랜잭션 전체가 abort돼 재시도까지 함께 실패한다.
 */
@Component
@RequiredArgsConstructor
public class VerificationRateLimitCreator {

    private final VerificationRateLimitRepository repository;

    /**
     * 이미 같은 행이 있으면 {@link RateLimitRowExistsException}으로 빠져나간다.
     * 충돌을 잡고 정상 반환하면 이 트랜잭션은 이미 rollback-only여서 커밋 시점에 터진다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createFirstAttempt(String rateKey, Instant windowStart) {
        try {
            repository.saveAndFlush(VerificationRateLimit.firstAttempt(rateKey, windowStart));
        } catch (DataIntegrityViolationException alreadyCreated) {
            throw new RateLimitRowExistsException(alreadyCreated);
        }
    }
}
