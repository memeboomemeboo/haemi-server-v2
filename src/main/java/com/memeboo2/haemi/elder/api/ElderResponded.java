package com.memeboo2.haemi.elder.api;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

/**
 * PR #14에서 저장된 Spring Modulith 이벤트 publication의 역직렬화 호환 타입.
 * 새 발행 코드는 common.event.ElderResponded를 사용한다.
 */
@Deprecated(forRemoval = false)
@Externalized
public record ElderResponded(UUID memoryId, UUID elderId) {}
