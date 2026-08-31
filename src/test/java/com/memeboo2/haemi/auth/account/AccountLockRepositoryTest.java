package com.memeboo2.haemi.auth.account;

import com.memeboo2.haemi.auth.account.domain.Account;
import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * incrementLoginFailure의 잠금·리셋 원자 UPDATE를 실제 DB에 대해 검증한다.
 * 특히 잠금 만료 후 카운터가 1로 리셋되면서 lockedUntil도 함께 해제되어,
 * 이후 maxAttempts회 실패 시 정상적으로 재잠금되는지를 확인한다(#120 회귀 방지).
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AccountLockRepositoryTest {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration LOCK = Duration.ofMinutes(15);
    private static final Instant T0 = Instant.parse("2026-08-31T00:00:00Z");

    @Autowired AccountRepository accountRepository;

    private String createGuardian() {
        String loginId = "lock_test_" + UUID.randomUUID().toString().substring(0, 8);
        accountRepository.saveAndFlush(
                Account.guardian("테스트", loginId, "hash", "1990-01-01", null, null, "pinHash"));
        return loginId;
    }

    private void fail(String loginId, Instant now) {
        accountRepository.incrementLoginFailure(loginId, MAX_ATTEMPTS, now.plus(LOCK), now);
    }

    @Test
    void maxAttempts회_실패하면_잠긴다() {
        String loginId = createGuardian();

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            fail(loginId, T0);
        }

        Account account = accountRepository.findByLoginId(loginId).orElseThrow();
        assertThat(account.getFailedLoginAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(account.isLocked(T0.plus(Duration.ofMinutes(1)))).isTrue();
    }

    @Test
    void 잠금이_만료된_뒤_실패하면_카운터가_1로_리셋되고_잠금도_해제된다() {
        String loginId = createGuardian();
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            fail(loginId, T0);
        }

        // 잠금 만료 후 첫 실패
        Instant afterExpiry = T0.plus(LOCK).plus(Duration.ofMinutes(1));
        fail(loginId, afterExpiry);

        Account account = accountRepository.findByLoginId(loginId).orElseThrow();
        assertThat(account.getFailedLoginAttempts()).isEqualTo(1);
        // 핵심: lockedUntil이 NULL로 해제되어야 무한 1-리셋 루프가 생기지 않는다.
        assertThat(account.isLocked(afterExpiry)).isFalse();
    }

    @Test
    void 잠금_만료_후_다시_maxAttempts회_실패하면_재잠금된다() {
        String loginId = createGuardian();
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            fail(loginId, T0);
        }

        // 잠금 만료 후 maxAttempts회 실패 → 재잠금되어야 한다.
        Instant afterExpiry = T0.plus(LOCK).plus(Duration.ofMinutes(1));
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            fail(loginId, afterExpiry);
        }

        Account account = accountRepository.findByLoginId(loginId).orElseThrow();
        assertThat(account.getFailedLoginAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(account.isLocked(afterExpiry.plus(Duration.ofMinutes(1)))).isTrue();
    }
}
