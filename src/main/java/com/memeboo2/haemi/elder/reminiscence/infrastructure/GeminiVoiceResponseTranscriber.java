package com.memeboo2.haemi.elder.reminiscence.infrastructure;

import com.memeboo2.haemi.elder.reminiscence.application.GeminiProperties;
import com.memeboo2.haemi.elder.response.application.TranscriptGenerationException;
import com.memeboo2.haemi.elder.response.application.VoiceResponseTranscriber;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;

/** Gemini {@code :generateContent}에 짧은 음성을 inline data로 전달하는 STT 어댑터. */
class GeminiVoiceResponseTranscriber implements VoiceResponseTranscriber {

    private static final String TRANSCRIPT_PROMPT = """
            이 음성에서 실제로 말한 내용을 한국어 원문 그대로 전사하세요.
            설명, 요약, 화자 표기, 따옴표, Markdown을 덧붙이지 말고 전사 텍스트만 반환하세요.
            음성을 알아들을 수 없으면 빈 문자열을 반환하세요.
            """;

    private final RestClient restClient;
    private final GeminiProperties props;
    private final Semaphore permits;

    GeminiVoiceResponseTranscriber(RestClient restClient, GeminiProperties props) {
        this.restClient = restClient;
        this.props = props;
        if (props.inlineAudioMaxBytes() <= 0 || props.maxConcurrentAudioRequests() <= 0) {
            throw new IllegalArgumentException("Gemini 음성 전사 설정값은 0보다 커야 합니다.");
        }
        this.permits = new Semaphore(props.maxConcurrentAudioRequests());
    }

    @Override
    public String transcribe(String contentType, byte[] audio) {
        if (audio == null || audio.length == 0) {
            throw new TranscriptGenerationException("전사할 음성 데이터가 없습니다.");
        }
        if (audio.length > props.inlineAudioMaxBytes()) {
            throw new TranscriptGenerationException("음성 파일이 Gemini inline 요청 최대 크기를 초과했습니다.");
        }

        boolean acquired = false;
        try {
            permits.acquire();
            acquired = true;
            Map<String, Object> inlineData = Map.of(
                    "mime_type", normalizeContentType(contentType),
                    "data", Base64.getEncoder().encodeToString(audio));
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(
                            Map.of("text", TRANSCRIPT_PROMPT),
                            Map.of("inline_data", inlineData)))),
                    "generationConfig", Map.of("temperature", 0));

            Map<?, ?> response = restClient.post()
                    .uri("/models/{model}:generateContent", props.model())
                    .header("x-goog-api-key", props.apiKey())
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            String transcript = extractText(response);
            if (transcript == null || transcript.isBlank()) {
                throw new TranscriptGenerationException("Gemini가 유효한 음성 전사를 반환하지 않았습니다.");
            }
            return transcript.strip();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranscriptGenerationException("Gemini 음성 전사 대기 중 인터럽트되었습니다.", e);
        } catch (RestClientException e) {
            throw new TranscriptGenerationException("Gemini 음성 전사 호출에 실패했습니다.", e);
        } finally {
            if (acquired) {
                permits.release();
            }
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new TranscriptGenerationException("음성 Content-Type이 없습니다.");
        }
        String normalized = contentType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            // 앱/브라우저가 M4A 컨테이너를 audio/mp4로 표기할 수 있지만 Gemini 입력명은 audio/m4a다.
            case "audio/mp4" -> "audio/m4a";
            case "audio/aac", "audio/ogg", "audio/webm" -> normalized;
            default -> throw new TranscriptGenerationException("Gemini가 지원하지 않는 음성 Content-Type입니다.");
        };
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> response) {
        if (response == null || !(response.get("candidates") instanceof List<?> candidates)) {
            return null;
        }
        List<String> texts = new ArrayList<>();
        for (Object candidateObject : candidates) {
            if (!(candidateObject instanceof Map<?, ?> candidate)
                    || !(candidate.get("content") instanceof Map<?, ?> content)
                    || !(content.get("parts") instanceof List<?> parts)) {
                continue;
            }
            for (Object partObject : parts) {
                if (partObject instanceof Map<?, ?> part && part.get("text") instanceof String text && !text.isBlank()) {
                    texts.add(text.strip());
                }
            }
        }
        return texts.isEmpty() ? null : String.join("\n", texts);
    }
}
