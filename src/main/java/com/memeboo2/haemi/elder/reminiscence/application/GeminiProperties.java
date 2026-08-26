package com.memeboo2.haemi.elder.reminiscence.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Gemini 연동 설정. {@code application.yaml}의 {@code haemi.ai.gemini} 블록을 바인딩한다.
 * {@code apiKey}가 비어 있으면 실제 호출 대신 템플릿 기반 대체 생성기가 등록된다.
 */
@ConfigurationProperties(prefix = "haemi.ai.gemini")
public record GeminiProperties(
        @DefaultValue("") String apiKey,
        @DefaultValue("gemini-2.0-flash") String model,
        @DefaultValue("https://generativelanguage.googleapis.com/v1beta") String baseUrl,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("20s") Duration readTimeout
) {
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
