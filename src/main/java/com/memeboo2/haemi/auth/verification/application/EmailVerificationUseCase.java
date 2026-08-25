package com.memeboo2.haemi.auth.verification.application;

import com.memeboo2.haemi.auth.credential.PasswordService;
import com.memeboo2.haemi.auth.verification.domain.EmailVerification;
import com.memeboo2.haemi.auth.verification.infrastructure.EmailVerificationRepository;
import com.memeboo2.haemi.auth.verification.infrastructure.VerificationRateLimitRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationUseCase {

    private static final long EXPIRY_SECONDS = 5 * 60;

    private final EmailVerificationRepository repository;
    private final VerificationRateLimitRepository rateLimitRepository;
    private final PasswordService passwordService;
    private final VerificationCodeGenerator codeGenerator;
    private final EmailSender emailSender;
    private final HaemiClock clock;
    private final EmailVerificationProperties properties;
    private final VerificationFailureRecorder verificationFailureRecorder;

    /**
     * 발송 제한은 DB의 고정 윈도우 카운터를 원자적으로 올려 판정한다.
     * count-then-insert나 프로세스 로컬 락은 동시 요청·다중 인스턴스에서 제한을 그대로 우회한다.
     */
    @Transactional
    public UUID request(String rawEmail) {
        String email = normalize(rawEmail);
        Instant now = clock.now();
        Instant windowStart = windowStart(now);
        String rateKey = rateKey(email);
        rateLimitRepository.increment(rateKey, windowStart);
        Integer attempts = rateLimitRepository.findAttemptCount(rateKey, windowStart);
        if (attempts != null && attempts > properties.maxResendPerWindow()) {
            throw new DomainException(ErrorCode.AUTH_VERIFICATION_RESEND_LIMITED);
        }

        String code = codeGenerator.nextCode();
        EmailVerification verification = repository.save(
                EmailVerification.pending(email, passwordService.encode(code), now.plusSeconds(EXPIRY_SECONDS)));
        emailSender.sendVerificationCode(email, code);
        return verification.getId();
    }

    @Transactional
    public void confirm(UUID verificationId, String code) {
        EmailVerification verification = find(verificationId);
        if (verification.isLocked(properties.maxConfirmAttempts())) {
            throw new DomainException(ErrorCode.AUTH_VERIFICATION_LOCKED);
        }
        if (!passwordService.matches(code, verification.getCodeHash())) {
            int failCount = verificationFailureRecorder.recordFailure(verificationId);
            if (failCount >= properties.maxConfirmAttempts()) {
                throw new DomainException(ErrorCode.AUTH_VERIFICATION_LOCKED);
            }
            throw new DomainException(ErrorCode.INVALID_INPUT, "인증번호가 올바르지 않습니다.");
        }
        verification.markVerified(clock.now());
    }

    /** 회원가입 트랜잭션에서 검증된 이메일 인증을 1회 소비한다. */
    @Transactional
    public void consumeVerified(UUID verificationId, String email) {
        find(verificationId).consumeFor(normalize(email), clock.now());
    }

    private EmailVerification find(UUID verificationId) {
        return repository.findById(verificationId)
                .orElseThrow(() -> new DomainException(ErrorCode.EMAIL_VERIFICATION_REQUIRED,
                        "이메일 인증이 필요합니다."));
    }

    /** 대소문자만 다른 주소가 서로 다른 제한 버킷을 갖지 않도록 정규화한다. */
    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * rate_key는 VARCHAR(255)이므로 최대 길이(255)인 이메일을 그대로 붙이면 컬럼을 넘긴다.
     * 주소를 해시해 길이를 고정한다.
     */
    private String rateKey(String email) {
        return "email-verification:" + sha256(email);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 고정 윈도우 시작 시각 (resendWindowSeconds 단위로 내림). */
    private Instant windowStart(Instant now) {
        long window = properties.resendWindowSeconds();
        return Instant.ofEpochSecond(now.getEpochSecond() / window * window);
    }
}
