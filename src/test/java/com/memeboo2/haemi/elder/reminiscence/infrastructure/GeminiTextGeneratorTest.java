package com.memeboo2.haemi.elder.reminiscence.infrastructure;

import com.memeboo2.haemi.elder.reminiscence.application.GeminiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GeminiTextGeneratorTest {

    @Mock RestClient restClient;
    @Mock RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock RestClient.RequestBodySpec requestBodySpec;
    @Mock RestClient.ResponseSpec responseSpec;

    private GeminiProperties props;
    private GeminiTextGenerator generator;

    @BeforeEach
    void setUp() {
        props = new GeminiProperties("test-api-key", "gemini-2.0-flash",
                "https://example.invalid", Duration.ofSeconds(5), Duration.ofSeconds(20), 12_582_912L, 2);
        generator = new GeminiTextGenerator(restClient, props);

        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void 정상_응답이면_텍스트를_추출해_반환한다() {
        Map<String, Object> response = Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", "  옛 추억이 떠오르네요  "))))));
        given(responseSpec.body(Map.class)).willReturn(response);

        String result = generator.generate("프롬프트");

        assertThat(result).isEqualTo("옛 추억이 떠오르네요");
    }

    @Test
    void 응답에_candidates가_없으면_대체_문구를_반환한다() {
        given(responseSpec.body(Map.class)).willReturn(Map.of("candidates", List.of()));

        String result = generator.generate("프롬프트");

        assertThat(result).isNotBlank();
        assertThat(result).contains("기억");
    }

    @Test
    void 응답이_null이면_대체_문구를_반환한다() {
        given(responseSpec.body(Map.class)).willReturn(null);

        String result = generator.generate("프롬프트");

        assertThat(result).isNotBlank();
    }

    @Test
    void 텍스트가_공백뿐이면_대체_문구를_반환한다() {
        Map<String, Object> response = Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", "   "))))));
        given(responseSpec.body(Map.class)).willReturn(response);

        String result = generator.generate("프롬프트");

        assertThat(result).isNotBlank();
        assertThat(result).doesNotContain("   ");
    }

    @Test
    void 호출이_실패하면_예외를_삼키고_대체_문구를_반환한다() {
        given(responseSpec.body(Map.class)).willThrow(new RestClientException("boom"));

        String result = generator.generate("프롬프트");

        assertThat(result).isNotBlank();
    }

    @Test
    void isLive는_true를_반환한다() {
        assertThat(generator.isLive()).isTrue();
    }
}
