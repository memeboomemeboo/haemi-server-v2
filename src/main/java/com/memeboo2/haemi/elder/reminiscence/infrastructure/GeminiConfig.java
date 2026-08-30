package com.memeboo2.haemi.elder.reminiscence.infrastructure;

import com.memeboo2.haemi.elder.reminiscence.application.AiTextGenerator;
import com.memeboo2.haemi.elder.reminiscence.application.GeminiProperties;
import com.memeboo2.haemi.elder.response.application.VoiceResponseTranscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Gemini 어댑터 등록. API 키가 없을 때 음성 전사는 임의 텍스트를 만들지 않고 실패 처리한다. */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    private static final Logger log = LoggerFactory.getLogger(GeminiConfig.class);

    @Bean
    AiTextGenerator aiTextGenerator(GeminiProperties props) {
        if (!props.hasApiKey()) {
            log.info("Gemini API 키 미설정 — 템플릿 기반 대체 생성기를 사용합니다.");
            return new TemplateAiTextGenerator();
        }
        return new GeminiTextGenerator(restClient(props), props);
    }

    @Bean
    VoiceResponseTranscriber voiceResponseTranscriber(GeminiProperties props) {
        if (!props.hasApiKey()) {
            log.warn("Gemini API 키 미설정 — 음성 답변 전사를 실행할 수 없습니다.");
            return new UnavailableVoiceResponseTranscriber();
        }
        return new GeminiVoiceResponseTranscriber(restClient(props), props);
    }

    private RestClient restClient(GeminiProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) props.connectTimeout().toMillis());
        factory.setReadTimeout((int) props.readTimeout().toMillis());
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.baseUrl())
                .build();
    }
}
