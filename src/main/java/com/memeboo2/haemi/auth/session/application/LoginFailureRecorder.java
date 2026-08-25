package com.memeboo2.haemi.auth.session.application;

import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 로그인 실패 카운터를 별도 트랜잭션에서 원자적으로 올린다.
 * - 별도 트랜잭션인 이유: LoginUseCase는 실패 시 DomainException을 던져 자신의 트랜잭션이 롤백되므로,
 *   같은 트랜잭션 안에서 카운터를 올리면 그 변경도 함께 사라져 계정 잠금이 동작하지 않는다.
 * - 원자적 UPDATE인 이유: 엔티티를 읽어 +1 하면 동시 실패 요청이 서로의 증가분을 덮어써
 *   임계값에 도달하지 못하고 잠금이 우회된다.
 */
@Component
@RequiredArgsConstructor
public class LoginFailureRecorder {

    private final AccountRepository accountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String loginId, Instant now, int maxFailedAttempts, long lockDurationSeconds) {
        accountRepository.incrementLoginFailure(loginId, maxFailedAttempts, now.plusSeconds(lockDurationSeconds));
    }
}
