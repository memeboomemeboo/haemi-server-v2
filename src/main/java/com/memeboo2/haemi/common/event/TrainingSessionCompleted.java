package com.memeboo2.haemi.common.event;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;
import java.util.UUID;

/**
 * elder/training이 그날 인지 훈련 세션을 완료했을 때 발행한다.
 * 발행처는 CompleteTrainingSessionUseCase, 소비처는 elder/attendance다.
 * 세션 도메인(#37)이 들어오면 발행 지점만 그쪽으로 옮긴다.
 */
@Externalized
public record TrainingSessionCompleted(UUID elderId, LocalDate sessionDate) {}
