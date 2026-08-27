package com.memeboo2.haemi.guardian.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 보호자 홈 타임라인에서 어르신의 최초 추억 열람 시각을 읽는 공개 계약이다. */
public interface MemoryViewActivityQuery {

    List<MemoryViewActivity> firstViewedBetween(UUID elderId, Instant from, Instant to);

    record MemoryViewActivity(UUID memoryId, Instant firstViewedAt) {}
}
