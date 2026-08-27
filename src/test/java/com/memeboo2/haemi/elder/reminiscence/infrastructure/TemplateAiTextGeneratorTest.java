package com.memeboo2.haemi.elder.reminiscence.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** TemplateAiTextGenerator의 대체(fallback) 문구 생성 단위 테스트. */
class TemplateAiTextGeneratorTest {

    private final TemplateAiTextGenerator generator = new TemplateAiTextGenerator();

    @Test
    void 프롬프트와_무관하게_고정된_회상_문구를_반환한다() {
        String result = generator.generate("아무 프롬프트");

        assertThat(result).contains("따뜻한 기억");
        assertThat(result).isNotBlank();
    }

    @Test
    void 빈_프롬프트를_전달해도_동일한_문구를_반환한다() {
        String result1 = generator.generate("");
        String result2 = generator.generate("다른 프롬프트");

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void null_프롬프트를_전달해도_예외없이_문구를_반환한다() {
        String result = generator.generate(null);

        assertThat(result).isNotBlank();
    }

    @Test
    void isLive는_항상_false를_반환한다() {
        assertThat(generator.isLive()).isFalse();
    }
}
