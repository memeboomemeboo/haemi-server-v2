package com.memeboo2.haemi.common.event;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 보호자가 하루 한마디를 보냈을 때 발행한다 (SendDailyCareUseCase).
 * 현재 인프로세스 소비자는 없다 — 외부 발행(@Externalized)을 통한 알림·분석 등
 * 후속 소비처를 위한 계약으로만 유지한다. 소비처가 생기면 리스너를 추가한다.
 */
@Externalized
public record GreetingSent(UUID dailyCareId, UUID guardianId, UUID elderId, LocalDate careDate) {}
