package com.memeboo2.haemi.auth.api;

import com.memeboo2.haemi.common.security.JwtPrincipal;
import com.memeboo2.haemi.common.security.TokenVerifier;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenVerifier implements TokenVerifier {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtTokenVerifier(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Optional<JwtPrincipal> verify(String token) {
        if (!jwtTokenProvider.isValid(token)) {
            return Optional.empty();
        }
        Claims claims = jwtTokenProvider.parse(token);
        UUID userId = UUID.fromString(claims.getSubject());
        String role = claims.get("role", String.class);
        // refresh 토큰은 role 클레임이 없다. 인증 헤더에 실려 오면 SimpleGrantedAuthority(null)이
        // IllegalArgumentException(500)을 던지므로, 여기서 미인증(401)으로 거부한다.
        if (role == null || role.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new JwtPrincipal(userId, role));
    }
}
