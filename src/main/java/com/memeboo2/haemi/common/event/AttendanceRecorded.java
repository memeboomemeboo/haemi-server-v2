package com.memeboo2.haemi.common.event;

import com.memeboo2.haemi.common.attendance.ActivityType;
import org.springframework.modulith.events.Externalized;

import java.time.LocalDate;
import java.util.UUID;

/**
 * elder/attendance가 하루 참여 기록을 저장했을 때 발행한다. 그날 특정 활동 종류가
 * 처음 기록될 때마다 발행된다 (같은 종류를 여러 번 해도 1회).
 * guardian/report는 이 사실만 소비해 자체 스냅샷을 멱등 적재하고,
 * 스트릭·상태는 스냅샷 조회 시 계산한다 (가변 집계 숫자는 이벤트에 담지 않는다).
 */
@Externalized
public record AttendanceRecorded(UUID elderId, LocalDate participationDate, ActivityType activityType) {}
