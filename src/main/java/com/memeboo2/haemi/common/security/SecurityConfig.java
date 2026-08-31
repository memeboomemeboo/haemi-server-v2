package com.memeboo2.haemi.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.MediaType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    private static final ObjectMapper ERROR_RESPONSE_MAPPER = new ObjectMapper();

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TokenVerifier tokenVerifier,
                                           Environment environment,
                                           CorsConfigurationSource corsConfigurationSource) throws Exception {
        // prod가 아닌 환경에서만 로컬 스토리지 엔드포인트를 개방한다.
        // prod에는 LocalStorageController 자체가 등록되지 않으므로(@Profile("!prod")) 규칙도 함께 제거한다.
        boolean localStorageEnabled = !environment.matchesProfiles("prod");
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth
                .requestMatchers("/api/v1/auth/guardians/register", "/api/v1/auth/login-id/availability",
                        "/api/v1/auth/login", "/api/v1/auth/refresh",
                        "/api/v1/auth/email-verifications/**",
                        "/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                // LocalStorageAdapter가 만드는 presigned URL을 개발 환경에서만 재현한다.
                if (localStorageEnabled) {
                    auth.requestMatchers("/api/v1/internal/storage/**").permitAll();
                }
                auth
                .requestMatchers("/api/v1/guardian/**").hasRole("GUARDIAN")
                .requestMatchers("/api/v1/elder/**").hasRole("ELDER")
                .anyRequest().authenticated();
            })
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, exception) ->
                            writeError(response, ErrorCode.UNAUTHENTICATED))
                    .accessDeniedHandler((request, response, exception) ->
                            writeError(response, ErrorCode.ROLE_NOT_ALLOWED)))
            .addFilterBefore(new JwtAuthenticationFilter(tokenVerifier),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ERROR_RESPONSE_MAPPER.writeValue(response.getOutputStream(),
                ApiResponse.error(errorCode.name(), errorCode.getDefaultMessage()));
    }
}
