package com.memeboo2.haemi.auth.session.application;

import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 로그인 실패 카운터를 별도 트랜잭션으로 커밋한다.
 * LoginUseCase는 실패 시 DomainException을 던져 자신의 트랜잭션이 롤백되므로,
 * 같은 트랜잭션 안에서 카운터를 올리면 그 변경도 함께 사라져 계정 잠금이 동작하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class LoginFailureRecorder {

    private final AccountRepository accountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String loginId, Instant now, int maxFailedAttempts, long lockDurationSeconds) {
        accountRepository.findByLoginId(loginId)
                .ifPresent(account -> account.recordLoginFailure(now, maxFailedAttempts, lockDurationSeconds));
    }
}
