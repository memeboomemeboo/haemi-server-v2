package com.memeboo2.haemi.guardian.family.infrastructure;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** SecureInviteCodeGenerator의 코드 형식 및 무작위성 단위 테스트. */
class SecureInviteCodeGeneratorTest {

    private static final String CONFUSING_CHARS = "0O1IL";

    private final SecureInviteCodeGenerator generator = new SecureInviteCodeGenerator();

    @RepeatedTest(20)
    void 항상_8자리_코드를_생성한다() {
        String code = generator.nextCode();

        assertThat(code).hasSize(8);
    }

    @RepeatedTest(20)
    void 혼동되는_문자를_포함하지_않는다() {
        String code = generator.nextCode();

        for (char c : CONFUSING_CHARS.toCharArray()) {
            assertThat(code).doesNotContain(String.valueOf(c));
        }
    }

    @RepeatedTest(20)
    void 대문자와_숫자로만_구성된다() {
        String code = generator.nextCode();

        assertThat(code).matches("[A-Z0-9]{8}");
    }

    @Test
    void 연속_생성시_대체로_서로_다른_코드를_생성한다() {
        Set<String> codes = new HashSet<>();
        IntStream.range(0, 100).forEach(i -> codes.add(generator.nextCode()));

        assertThat(codes.size()).isGreaterThan(90);
    }
}
