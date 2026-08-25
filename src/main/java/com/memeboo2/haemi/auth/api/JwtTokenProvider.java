package com.memeboo2.haemi.auth.api;

import com.memeboo2.haemi.auth.account.domain.AccountRole;
import com.memeboo2.haemi.auth.session.application.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class JwtTokenProvider {

    private final SecretKey key;
    private final JwtProperties props;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UUID userId, AccountRole role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.accessTokenValidity().toMillis());
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", "ROLE_" + role.name())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(UUID userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.refreshTokenValidity().toMillis());
        return Jwts.builder()
                .subject(userId.toString())
                // jti(랜덤 식별자): 같은 계정이 같은 초에 여러 기기에서 로그인해도 토큰 문자열이 겹치지 않게 한다.
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
