package com.memeboo2.haemi.elder.reminiscence.infrastructure;

import com.memeboo2.haemi.elder.reminiscence.application.GeminiProperties;
import com.memeboo2.haemi.elder.response.application.TranscriptGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GeminiVoiceResponseTranscriberTest {

    @Mock RestClient restClient;
    @Mock RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock RestClient.RequestBodySpec requestBodySpec;
    @Mock RestClient.ResponseSpec responseSpec;

    private GeminiVoiceResponseTranscriber transcriber;

    @BeforeEach
    void setUp() {
        GeminiProperties props = new GeminiProperties("test-api-key", "gemini-2.0-flash",
                "https://example.invalid", Duration.ofSeconds(5), Duration.ofSeconds(20), 12_582_912L, 2);
        transcriber = new GeminiVoiceResponseTranscriber(restClient, props);

        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void Gemini에_음성_원본을_inline_data로_전송하고_전사를_반환한다() {
        byte[] audio = new byte[]{1, 2, 3};
        given(responseSpec.body(Map.class)).willReturn(Map.of(
                "candidates", List.of(Map.of("content", Map.of(
                        "parts", List.of(Map.of("text", "  안녕하세요  ")))))));

        String transcript = transcriber.transcribe("audio/mp4", audio);

        assertThat(transcript).isEqualTo("안녕하세요");
        verify(requestBodySpec).header("x-goog-api-key", "test-api-key");
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodySpec).body(bodyCaptor.capture());

        Map<?, ?> body = (Map<?, ?>) bodyCaptor.getValue();
        List<?> contents = (List<?>) body.get("contents");
        Map<?, ?> content = (Map<?, ?>) contents.getFirst();
        List<?> parts = (List<?>) content.get("parts");
        Map<?, ?> inlineData = (Map<?, ?>) ((Map<?, ?>) parts.get(1)).get("inline_data");
        assertThat(inlineData.get("mime_type")).isEqualTo("audio/m4a");
        assertThat(inlineData.get("data")).isEqualTo(Base64.getEncoder().encodeToString(audio));
    }

    @Test
    void Gemini_실패를_임의_전사로_대체하지_않는다() {
        given(responseSpec.body(Map.class)).willThrow(new RestClientException("boom"));

        assertThatThrownBy(() -> transcriber.transcribe("audio/aac", new byte[]{1}))
                .isInstanceOf(TranscriptGenerationException.class)
                .hasMessageContaining("Gemini 음성 전사 호출");
    }

    @Test
    void 빈_음성은_Gemini를_호출하지_않는다() {
        assertThatThrownBy(() -> transcriber.transcribe("audio/aac", new byte[0]))
                .isInstanceOf(TranscriptGenerationException.class)
                .hasMessageContaining("음성 데이터");
    }

    @Test
    void null_음성은_Gemini를_호출하지_않는다() {
        assertThatThrownBy(() -> transcriber.transcribe("audio/aac", null))
                .isInstanceOf(TranscriptGenerationException.class)
                .hasMessageContaining("음성 데이터");
    }

    @Test
    void inline_크기를_넘는_음성은_Gemini를_호출하지_않는다() {
        GeminiProperties tinyLimit = new GeminiProperties("test-api-key", "gemini-2.0-flash",
                "https://example.invalid", Duration.ofSeconds(5), Duration.ofSeconds(20), 1L, 2);
        GeminiVoiceResponseTranscriber limited = new GeminiVoiceResponseTranscriber(restClient, tinyLimit);

        assertThatThrownBy(() -> limited.transcribe("audio/aac", new byte[]{1, 2}))
                .isInstanceOf(TranscriptGenerationException.class)
                .hasMessageContaining("최대 크기");
    }

    @Test
    void 허용하지_않은_음성_Content_Type은_Gemini를_호출하지_않는다() {
        assertThatThrownBy(() -> transcriber.transcribe("audio/mpeg", new byte[]{1}))
                .isInstanceOf(TranscriptGenerationException.class)
                .hasMessageContaining("지원하지 않는");
    }

    @Test
    void Gemini가_candidates를_반환하지_않으면_전사_실패로_처리한다() {
        given(responseSpec.body(Map.class)).willReturn(Map.of("candidates", List.of()));

        assertThatThrownBy(() -> transcriber.transcribe("audio/aac", new byte[]{1}))
                .isInstanceOf(TranscriptGenerationException.class)
                .hasMessageContaining("유효한 음성 전사");
    }

    @Test
    void Gemini_응답_형식이_잘못되면_전사_실패로_처리한다() {
        given(responseSpec.body(Map.class)).willReturn(Map.of("candidates", List.of("invalid")));

        assertThatThrownBy(() -> transcriber.transcribe("audio/aac", new byte[]{1}))
                .isInstanceOf(TranscriptGenerationException.class)
                .hasMessageContaining("유효한 음성 전사");
    }

    @Test
    void Gemini_응답의_텍스트가_공백이면_전사_실패로_처리한다() {
        given(responseSpec.body(Map.class)).willReturn(Map.of(
                "candidates", List.of(Map.of("content", Map.of(
                        "parts", List.of(Map.of("text", "  ")))))));

        assertThatThrownBy(() -> transcriber.transcribe("audio/aac", new byte[]{1}))
                .isInstanceOf(TranscriptGenerationException.class)
                .hasMessageContaining("유효한 음성 전사");
    }

    @Test
    void 잘못된_동시_요청_설정은_생성시_거부한다() {
        GeminiProperties invalid = new GeminiProperties("test-api-key", "gemini-2.0-flash",
                "https://example.invalid", Duration.ofSeconds(5), Duration.ofSeconds(20), 1L, 0);

        assertThatThrownBy(() -> new GeminiVoiceResponseTranscriber(restClient, invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 전사_대기_중_인터럽트되면_실패로_전환하고_인터럽트_상태를_보존한다() {
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> transcriber.transcribe("audio/aac", new byte[]{1}))
                    .isInstanceOf(TranscriptGenerationException.class)
                    .hasMessageContaining("인터럽트");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
