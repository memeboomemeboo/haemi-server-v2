package com.memeboo2.haemi.guardian.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** elder 모듈이 어르신 등록 정보를 조회하는 계약 (예: 등록일 기준 "함께한 일 수" 계산). */
public interface ElderQuery {

    record ElderInfo(UUID elderId, String name, Instant registeredAt) {}

    Optional<ElderInfo> findById(UUID elderId);
}
