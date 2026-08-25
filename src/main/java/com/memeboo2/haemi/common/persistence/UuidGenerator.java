package com.memeboo2.haemi.common.persistence;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

public final class UuidGenerator {

    private UuidGenerator() {}

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * UUID v7 — 시간 순 정렬 가능. 인덱스 단편화 완화. (RFC 9562)
     * Java 21에 UUID v7 내장 API가 없어 직접 생성.
     *
     * 랜덤 비트는 SecureRandom.nextLong()으로 채운다. Math.random()은
     * double 가수(52비트) 한계로 하위 비트가 소실되고 예측 가능하기 때문.
     */
    public static UUID generate() {
        long timestamp = Instant.now().toEpochMilli();
        // msb: [48비트 timestamp][4비트 version=0x7][12비트 random]
        long msb = (timestamp << 16) | 0x7000L | (RANDOM.nextLong() & 0x0FFFL);
        // lsb: [2비트 variant=0b10][62비트 random]
        long lsb = 0x8000000000000000L | (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL);
        return new UUID(msb, lsb);
    }
}
