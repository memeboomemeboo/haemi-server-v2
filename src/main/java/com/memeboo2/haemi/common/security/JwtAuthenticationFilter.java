package com.memeboo2.haemi.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "bearer ";

    private final TokenVerifier tokenVerifier;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.toLowerCase().startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            Optional<JwtPrincipal> principal = tokenVerifier.verify(token);
            if (principal.isPresent()) {
                JwtPrincipal p = principal.get();
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(p, null,
                                List.of(new SimpleGrantedAuthority(p.role())));
                SecurityContextHolder.getContext().setAuthentication(auth);

                if ("ROLE_GUARDIAN".equals(p.role())) {
                    request.setAttribute("guardianId", p.userId());
                } else if ("ROLE_ELDER".equals(p.role())) {
                    request.setAttribute("elderUserId", p.userId());
                }
            } else {
                log.debug("JWT 검증 실패 — 토큰이 만료되었거나 유효하지 않음 uri={}", request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }
}
