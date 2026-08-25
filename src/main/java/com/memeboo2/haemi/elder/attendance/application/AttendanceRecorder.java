package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.common.persistence.UuidGenerator;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 활동 종류별 참여를 멱등하게 기록하고, 그날 그 종류가 처음 기록될 때만 AttendanceRecorded를 발행한다.
 * 4개 활동 리스너의 공통 경로.
 */
@Component
@RequiredArgsConstructor
public class AttendanceRecorder {

    private final DailyParticipationRepository repository;
    private final DailyParticipationWriter participationWriter;
    private final ApplicationEventPublisher publisher;

    // package-private: 같은 패키지의 신뢰된 이벤트 리스너만 호출한다. 사용자 유스케이스가 아니므로
    // elderId는 요청이 아니라 도메인 이벤트에서 오고, 접근 검증은 원래 행위 시점에 이미 끝났다.
    void record(UUID elderId, LocalDate date, ActivityType type) {
        boolean training = type == ActivityType.TRAINING;
        boolean greetingRead = type == ActivityType.GREETING_READ;
        boolean memoryViewed = type == ActivityType.MEMORY_VIEWED;
        boolean replied = type == ActivityType.REPLIED;

        // 원자적 부분 UPDATE — 다른 활동과 동시에 갱신돼도 각자 플래그만 OR로 켠다.
        int updated = repository.markActivity(elderId, date, training, greetingRead, memoryViewed, replied);
        if (updated == 0) {
            // 행이 없거나 이미 켜져 있음. 없으면 멱등 삽입 후 재시도한다 (동시 삽입은 한쪽만 성공).
            participationWriter.insertIfAbsent(UuidGenerator.generate(), elderId, date);
            updated = repository.markActivity(elderId, date, training, greetingRead, memoryViewed, replied);
        }

        // 1 = 해당 종류가 새로 켜짐 → 그때만 발행 (중복 발행 방지)
        if (updated > 0) {
            publisher.publishEvent(new AttendanceRecorded(elderId, date, type));
        }
    }
}
