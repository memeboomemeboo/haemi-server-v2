package com.memeboo2.haemi.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenVerifier tokenVerifier;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            tokenVerifier.verify(token).ifPresent(principal -> {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority(principal.role())));
                SecurityContextHolder.getContext().setAuthentication(auth);

                if ("ROLE_GUARDIAN".equals(principal.role())) {
                    request.setAttribute("guardianId", principal.userId());
                } else if ("ROLE_ELDER".equals(principal.role())) {
                    request.setAttribute("elderUserId", principal.userId());
                }
            });
        }
        filterChain.doFilter(request, response);
    }
}
