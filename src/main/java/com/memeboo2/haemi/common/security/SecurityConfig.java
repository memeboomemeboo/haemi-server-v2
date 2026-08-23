package com.memeboo2.haemi.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TokenVerifier tokenVerifier) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/guardians/register", "/api/v1/auth/login", "/api/v1/auth/phone-verifications/**",
                        "/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // LocalStorageAdapter가 만드는 presigned URL을 개발 환경에서 재현한다.
                .requestMatchers("/internal/storage/**").permitAll()
                .requestMatchers("/api/v1/guardian/**").hasRole("GUARDIAN")
                .requestMatchers("/api/v1/elder/**").hasRole("ELDER")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, exception) ->
                    response.sendError(HttpStatus.UNAUTHORIZED.value())))
            .addFilterBefore(new JwtAuthenticationFilter(tokenVerifier),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
