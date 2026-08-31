package com.memeboo2.haemi.elder.reminiscence.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Gemini 연동 설정. {@code application.yaml}의 {@code haemi.ai.gemini} 블록을 바인딩한다.
 * {@code apiKey}가 비어 있으면 회상 생성은 템플릿 기반 대체 생성기를 사용하고,
 * 음성 전사는 실패 상태로 남긴다. 음성 내용을 임의의 문구로 대체하면 안 된다.
 */
@ConfigurationProperties(prefix = "haemi.ai.gemini")
public record GeminiProperties(
        @DefaultValue("") String apiKey,
        @DefaultValue("gemini-3.5-flash") String model,
        @DefaultValue("https://generativelanguage.googleapis.com/v1beta") String baseUrl,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("20s") Duration readTimeout,
        @DefaultValue("12582912") long inlineAudioMaxBytes,
        @DefaultValue("2") int maxConcurrentAudioRequests
) {
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
