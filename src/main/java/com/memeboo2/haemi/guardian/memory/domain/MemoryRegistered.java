package com.memeboo2.haemi.guardian.memory.domain;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

/**
 * 보호자가 추억을 등록했을 때 발행한다 (RegisterMemoryUseCase).
 * 현재 인프로세스 소비자는 없다 — 외부 발행(@Externalized)을 통한 알림 등
 * 후속 소비처를 위한 계약으로만 유지한다. 소비처가 생기면 리스너를 추가한다.
 */
@Externalized
public record MemoryRegistered(UUID memoryId, UUID elderId, UUID guardianId) {}
