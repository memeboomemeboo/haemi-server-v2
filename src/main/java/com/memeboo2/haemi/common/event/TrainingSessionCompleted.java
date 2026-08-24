package com.memeboo2.haemi.common.event;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;
import java.util.UUID;

/**
 * elder/training이 그날 인지 훈련 세션을 완료했을 때 발행한다.
 * elder/training은 아직 세션 도메인이 없어 현재 발행처가 없다 —
 * elder/attendance는 이 계약을 소비할 준비만 갖춘 상태다.
 */
@Externalized
public record TrainingSessionCompleted(UUID elderId, LocalDate sessionDate) {}
