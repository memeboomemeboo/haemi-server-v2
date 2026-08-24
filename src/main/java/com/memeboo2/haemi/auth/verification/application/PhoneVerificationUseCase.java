package com.memeboo2.haemi.auth.verification.application;

import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.verification.domain.PhoneVerification;
import com.memeboo2.haemi.auth.verification.infrastructure.PhoneVerificationRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneVerificationUseCase {

    private static final long EXPIRY_SECONDS = 5 * 60;

    private final PhoneVerificationRepository repository;
    private final PasswordService passwordService;
    private final VerificationCodeGenerator codeGenerator;
    private final SmsSender smsSender;
    private final HaemiClock clock;
    private final PhoneVerificationProperties properties;

    @Transactional
    public UUID request(String phone) {
        Instant windowStart = clock.now().minusSeconds(properties.resendWindowSeconds());
        if (repository.countByPhoneAndCreatedAtAfter(phone, windowStart) >= properties.maxResendPerWindow()) {
            throw new DomainException(ErrorCode.AUTH_VERIFICATION_RESEND_LIMITED);
        }

        String code = codeGenerator.nextCode();
        Instant expiresAt = clock.now().plusSeconds(EXPIRY_SECONDS);
        PhoneVerification verification = repository.save(
                PhoneVerification.pending(phone, passwordService.encode(code), expiresAt));
        smsSender.sendVerificationCode(phone, code);
        return verification.getId();
    }

    @Transactional
    public void confirm(UUID verificationId, String code) {
        PhoneVerification verification = find(verificationId);
        if (verification.isLocked(properties.maxConfirmAttempts())) {
            throw new DomainException(ErrorCode.AUTH_VERIFICATION_LOCKED);
        }
        if (!passwordService.matches(code, verification.getCodeHash())) {
            verification.recordFailedAttempt();
            throw new DomainException(ErrorCode.INVALID_INPUT, "인증번호가 올바르지 않습니다.");
        }
        verification.markVerified(clock.now());
    }

    /** 회원가입 트랜잭션에서 검증된 휴대폰 인증을 1회 소비한다. */
    @Transactional
    public void consumeVerified(UUID verificationId, String phone) {
        find(verificationId).consumeFor(phone, clock.now());
    }

    private PhoneVerification find(UUID verificationId) {
        return repository.findById(verificationId)
                .orElseThrow(() -> new DomainException(ErrorCode.PHONE_VERIFICATION_REQUIRED,
                        "휴대폰 인증이 필요합니다."));
    }
}
