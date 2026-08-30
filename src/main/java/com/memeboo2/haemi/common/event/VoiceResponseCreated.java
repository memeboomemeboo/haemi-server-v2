package com.memeboo2.haemi.common.event;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

/** 확정된 음성 답변의 비동기 전사를 시작하기 위한 최소 이벤트 계약. */
@Externalized
public record VoiceResponseCreated(UUID responseId, UUID mediaRefId) {}
