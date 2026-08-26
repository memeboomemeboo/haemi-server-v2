package com.memeboo2.haemi.elder.reminiscence.infrastructure;

import com.memeboo2.haemi.elder.reminiscence.application.AiTextGenerator;
import com.memeboo2.haemi.elder.reminiscence.application.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Gemini 어댑터 등록. API 키가 있으면 실호출 어댑터, 없으면 템플릿 대체 생성기. */
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
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) props.connectTimeout().toMillis());
        factory.setReadTimeout((int) props.readTimeout().toMillis());
        RestClient restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.baseUrl())
                .build();
        return new GeminiTextGenerator(restClient, props);
    }
}
