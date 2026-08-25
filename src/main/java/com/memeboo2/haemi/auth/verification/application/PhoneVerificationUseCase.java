package com.memeboo2.haemi.auth.verification.application;

import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.verification.domain.PhoneVerification;
import com.memeboo2.haemi.auth.verification.infrastructure.PhoneVerificationRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PhoneVerificationUseCase {

    private static final long EXPIRY_SECONDS = 5 * 60;

    private final PhoneVerificationRepository repository;
    private final PasswordService passwordService;
    private final VerificationCodeGenerator codeGenerator;
    private final SmsSender smsSender;
    private final HaemiClock clock;
    private final PhoneVerificationProperties properties;
    private final VerificationFailureRecorder verificationFailureRecorder;
    private final TransactionTemplate transactionTemplate;

    /** 전화번호별 재발송 검사~저장 구간을 직렬화한다 (단일 인스턴스 배포 전제). */
    private final ConcurrentHashMap<String, Object> resendLocks = new ConcurrentHashMap<>();

    public PhoneVerificationUseCase(PhoneVerificationRepository repository, PasswordService passwordService,
            VerificationCodeGenerator codeGenerator, SmsSender smsSender, HaemiClock clock,
            PhoneVerificationProperties properties, VerificationFailureRecorder verificationFailureRecorder,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.passwordService = passwordService;
        this.codeGenerator = codeGenerator;
        this.smsSender = smsSender;
        this.clock = clock;
        this.properties = properties;
        this.verificationFailureRecorder = verificationFailureRecorder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * count-then-insert 구간을 phone별 락 + 별도 트랜잭션 커밋으로 원자화한다.
     * {@code @Transactional} 프록시만으로는 두 요청이 모두 count 통과 후 각자 insert할 수 있어
     * 재발송 제한(5회)이 동시 요청 앞에서 우회된다.
     */
    public UUID request(String phone) {
        Object lock = resendLocks.computeIfAbsent(phone, k -> new Object());
        synchronized (lock) {
            return transactionTemplate.execute(status -> {
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
            });
        }
    }

    @Transactional
    public void confirm(UUID verificationId, String code) {
        PhoneVerification verification = find(verificationId);
        if (verification.isLocked(properties.maxConfirmAttempts())) {
            throw new DomainException(ErrorCode.AUTH_VERIFICATION_LOCKED);
        }
        if (!passwordService.matches(code, verification.getCodeHash())) {
            verificationFailureRecorder.recordFailure(verificationId);
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
