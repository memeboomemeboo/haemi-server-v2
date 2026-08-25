package com.memeboo2.haemi.auth.verification.application;

import com.memeboo2.haemi.auth.verification.infrastructure.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 인증번호 실패 카운터를 별도 트랜잭션에서 원자적으로 올리고 증가 후 값을 돌려준다.
 * - 별도 트랜잭션인 이유: confirm()은 코드가 틀리면 예외를 던져 자기 트랜잭션을 롤백하므로,
 *   같은 트랜잭션에서 올린 카운터도 함께 사라진다.
 * - 원자적 UPDATE인 이유: 엔티티를 읽어 +1 하면 동시 오답 요청이 서로의 증가분을 덮어써
 *   제한 횟수가 실제로 누적되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class VerificationFailureRecorder {

    private final EmailVerificationRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailure(UUID verificationId) {
        repository.incrementFailCount(verificationId);
        Integer failCount = repository.findFailCount(verificationId);
        return failCount == null ? 0 : failCount;
    }
}
