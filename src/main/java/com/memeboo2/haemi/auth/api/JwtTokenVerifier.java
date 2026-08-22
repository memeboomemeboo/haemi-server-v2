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
        return Optional.of(new JwtPrincipal(userId, role));
    }
}
