package com.memeboo2.haemi.elder.api;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

/**
 * 어르신이 추억 앨범에 답변했을 때 발행되는 이벤트.
 * elder/api(공개 계약)에 위치 — guardian이 구독할 수 있도록 순환 없이 노출.
 */
@Externalized
public record ElderResponded(UUID memoryId, UUID elderId) {}
