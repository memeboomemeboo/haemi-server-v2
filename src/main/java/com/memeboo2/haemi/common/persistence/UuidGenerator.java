package com.memeboo2.haemi.common.persistence;

import java.time.Instant;
import java.util.UUID;

public final class UuidGenerator {

    private UuidGenerator() {}

    /**
     * UUID v7 — 시간 순 정렬 가능. 인덱스 단편화 완화.
     * Java 21에 UUID v7 내장 API가 없어 직접 생성.
     */
    public static UUID generate() {
        long timestamp = Instant.now().toEpochMilli();
        long msb = (timestamp << 16) | 0x7000L | (randomBits() & 0x0FFFL);
        long lsb = (0x8000000000000000L) | (randomBits() & 0x3FFFFFFFFFFFFFFFL);
        return new UUID(msb, lsb);
    }

    private static long randomBits() {
        return (long) (Math.random() * Long.MAX_VALUE);
    }
}
