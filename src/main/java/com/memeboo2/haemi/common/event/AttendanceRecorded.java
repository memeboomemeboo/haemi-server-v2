package com.memeboo2.haemi.common.event;

import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;
import java.util.UUID;

/**
 * elder/attendance가 하루 참여 기록을 저장했을 때 발행한다.
 * guardian/report는 이 사실만 소비해 자체 스냅샷을 멱등 적재하고,
 * 스트릭·상태는 스냅샷 조회 시 계산한다 (가변 숫자를 이벤트에 담지 않는다).
 */
@Externalized
public record AttendanceRecorded(UUID elderId, LocalDate participationDate) {}
