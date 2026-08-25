package com.memeboo2.haemi.auth;

import com.memeboo2.haemi.auth.api.JwtTokenProvider;
import com.memeboo2.haemi.auth.session.application.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(
            "test-secret-key-that-is-long-enough-for-hs256-signing-32b+",
            Duration.ofMinutes(30),
            Duration.ofDays(14)));

    @Test
    void refresh_토큰은_같은_사용자_같은_시각에도_jti로_매번_달라진다() {
        UUID userId = UUID.randomUUID();

        String first = provider.createRefreshToken(userId);
        String second = provider.createRefreshToken(userId);

        // jti(랜덤 claim)가 없으면 같은 초 발급 시 문자열이 동일해질 수 있다.
        assertThat(first).isNotEqualTo(second);
        assertThat(provider.isValid(first)).isTrue();
        assertThat(provider.isValid(second)).isTrue();
        assertThat(provider.parse(first).getId()).isNotBlank();
    }
}
