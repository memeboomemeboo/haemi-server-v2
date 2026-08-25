package com.memeboo2.haemi.common.event;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;
import java.util.UUID;

/**
 * elder/training이 그날 인지 훈련 세션을 완료했을 때 발행한다.
 * 발행처는 CIST 세션의 마지막 문항 완료이며, 소비처는 elder/attendance다.
 * 출석·리포트에는 일자별 참여 사실만 전달하고 세션 세부 결과는 training에 둔다.
 */
@Externalized
public record TrainingSessionCompleted(UUID elderId, LocalDate sessionDate) {}
