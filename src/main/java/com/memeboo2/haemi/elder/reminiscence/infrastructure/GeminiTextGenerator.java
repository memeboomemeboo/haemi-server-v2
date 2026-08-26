package com.memeboo2.haemi.elder.reminiscence.infrastructure;

import com.memeboo2.haemi.elder.reminiscence.application.AiTextGenerator;
import com.memeboo2.haemi.elder.reminiscence.application.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/** Gemini {@code :generateContent} 호출 어댑터 (동기, RestClient). */
class GeminiTextGenerator implements AiTextGenerator {

    private static final Logger log = LoggerFactory.getLogger(GeminiTextGenerator.class);

    private final RestClient restClient;
    private final GeminiProperties props;
    private final AiTextGenerator fallback = new TemplateAiTextGenerator();

    GeminiTextGenerator(RestClient restClient, GeminiProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    @Override
    public String generate(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        try {
            Map<?, ?> response = restClient.post()
                    .uri("/models/{model}:generateContent", props.model())
                    // API 키는 URL 쿼리(로그 노출 위험) 대신 헤더로 전달한다.
                    .header("x-goog-api-key", props.apiKey())
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            String text = extractText(response);
            if (text == null || text.isBlank()) {
                log.warn("Gemini 응답에서 텍스트를 찾지 못했습니다 — 대체 문구 사용");
                return fallback.generate(prompt);
            }
            return text.strip();
        } catch (RestClientException e) {
            log.warn("Gemini 호출 실패 — 대체 문구 사용: {}", e.toString());
            return fallback.generate(prompt);
        }
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                return null;
            }
            return (String) parts.get(0).get("text");
        } catch (ClassCastException | NullPointerException e) {
            return null;
        }
    }
}
