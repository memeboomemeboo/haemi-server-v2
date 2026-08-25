package com.memeboo2.haemi.common.persistence;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidGeneratorTest {

    @Test
    void 버전은_7이고_변형은_RFC_9562를_만족한다() {
        for (int i = 0; i < 1000; i++) {
            UUID uuid = UuidGenerator.generate();
            // version 필드(msb의 12~15비트)는 7
            assertThat(uuid.version()).isEqualTo(7);
            // variant 필드는 2 (RFC 4122/9562: 상위 2비트 0b10)
            assertThat(uuid.variant()).isEqualTo(2);
        }
    }

    @Test
    void 시간_순으로_증가한다() {
        UUID first = UuidGenerator.generate();
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        UUID later = UuidGenerator.generate();
        // v7은 상위 48비트가 밀리초 타임스탬프라 문자열 비교로도 시간 순 정렬된다
        assertThat(first.toString()).isLessThan(later.toString());
    }

    @Test
    void 대량_생성시_충돌이_없다() {
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            assertThat(seen.add(UuidGenerator.generate())).isTrue();
        }
    }
}
