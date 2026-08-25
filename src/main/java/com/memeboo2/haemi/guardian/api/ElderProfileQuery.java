package com.memeboo2.haemi.guardian.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** elder 모듈이 출석·콘텐츠 적합성 계산에만 쓰는 최소 어르신 프로필 조회 계약이다. */
public interface ElderProfileQuery {

    ElderProfile findById(UUID elderId);

    record ElderProfile(LocalDate birthDate, Instant registeredAt) {}
}
