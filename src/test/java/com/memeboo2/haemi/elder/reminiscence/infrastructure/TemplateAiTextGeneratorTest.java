package com.memeboo2.haemi.elder.reminiscence.infrastructure;

import com.memeboo2.haemi.elder.reminiscence.application.AiTextGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** TemplateAiTextGenerator의 대체(fallback) 문구 생성 단위 테스트. */
class TemplateAiTextGeneratorTest {

    private final TemplateAiTextGenerator generator = new TemplateAiTextGenerator();

    @Test
    void 프롬프트와_무관하게_고정된_회상_문구를_반환한다() {
        AiTextGenerator.Result result = generator.generate("아무 프롬프트");

        assertThat(result.text()).contains("따뜻한 기억");
        assertThat(result.text()).isNotBlank();
    }

    @Test
    void 빈_프롬프트를_전달해도_동일한_문구를_반환한다() {
        AiTextGenerator.Result result1 = generator.generate("");
        AiTextGenerator.Result result2 = generator.generate("다른 프롬프트");

        assertThat(result1.text()).isEqualTo(result2.text());
    }

    @Test
    void null_프롬프트를_전달해도_예외없이_문구를_반환한다() {
        AiTextGenerator.Result result = generator.generate(null);

        assertThat(result.text()).isNotBlank();
    }

    @Test
    void 대체_문구는_항상_live가_false다() {
        assertThat(generator.generate("아무 프롬프트").live()).isFalse();
    }
}
