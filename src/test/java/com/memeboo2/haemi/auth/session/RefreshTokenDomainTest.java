package com.memeboo2.haemi.auth.session;

import com.memeboo2.haemi.auth.session.domain.RefreshToken;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** RefreshToken의 of 팩토리와 만료 판정을 검증한다. */
class RefreshTokenDomainTest {

    @Test
    void of는_전달받은_값으로_리프레시_토큰을_생성한다() {
        UUID accountId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);

        RefreshToken token = RefreshToken.of(accountId, "device-1", "token-value", expiresAt);

        assertThat(token.getAccountId()).isEqualTo(accountId);
        assertThat(token.getDeviceId()).isEqualTo("device-1");
        assertThat(token.getToken()).isEqualTo("token-value");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getCreatedAt()).isNotNull();
    }

    @Test
    void of로_생성한_직후에는_id가_아직_없다() {
        RefreshToken token = RefreshToken.of(
                UUID.randomUUID(), "device-1", "token-value", Instant.now().plusSeconds(3600));

        assertThat(token.getId()).isNull();
    }

    @Test
    void isExpired는_만료_시각이_과거면_true다() {
        Instant now = Instant.parse("2026-08-31T00:00:00Z");
        RefreshToken token = RefreshToken.of(
                UUID.randomUUID(), "device-1", "token-value", now.minusSeconds(60));

        assertThat(token.isExpired(now)).isTrue();
    }

    @Test
    void isExpired는_만료_시각이_미래면_false다() {
        Instant now = Instant.parse("2026-08-31T00:00:00Z");
        RefreshToken token = RefreshToken.of(
                UUID.randomUUID(), "device-1", "token-value", now.plusSeconds(3600));

        assertThat(token.isExpired(now)).isFalse();
    }
}
