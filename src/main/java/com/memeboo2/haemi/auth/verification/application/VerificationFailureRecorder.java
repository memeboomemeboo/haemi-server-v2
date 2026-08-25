package com.memeboo2.haemi.auth.verification.application;

import com.memeboo2.haemi.auth.verification.infrastructure.PhoneVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 인증번호 실패 카운터를 별도 트랜잭션으로 커밋한다.
 * confirm()은 코드가 틀리면 DomainException을 던져 자신의 트랜잭션이 롤백되므로,
 * 같은 트랜잭션 안에서 카운터를 올리면 그 변경도 함께 사라져 5회 제한이 우회된다.
 */
@Component
@RequiredArgsConstructor
public class VerificationFailureRecorder {

    private final PhoneVerificationRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID verificationId) {
        repository.findById(verificationId).ifPresent(v -> v.recordFailedAttempt());
    }
}
