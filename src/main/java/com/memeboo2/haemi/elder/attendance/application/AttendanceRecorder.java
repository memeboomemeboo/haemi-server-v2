package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
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
    private final ApplicationEventPublisher publisher;

    // package-private: 같은 패키지의 신뢰된 이벤트 리스너만 호출한다. 사용자 유스케이스가 아니므로
    // elderId는 요청이 아니라 도메인 이벤트에서 오고, 접근 검증은 원래 행위 시점에 이미 끝났다.
    // 호출 리스너가 트랜잭션(@ApplicationModuleListener)을 열어주므로 변경 감지로 플러시된다.
    void record(UUID elderId, LocalDate date, ActivityType type) {
        DailyParticipation participation = repository
                .findByElderIdAndParticipationDate(elderId, date)
                .orElse(null);

        boolean newlyRecorded;
        if (participation == null) {
            // 동시 최초 삽입은 unique 제약으로 한쪽만 성공한다. 실패분은 이벤트 재전달로 갱신 경로를 탄다.
            DailyParticipation created = DailyParticipation.of(elderId, date);
            created.mark(type);
            repository.saveAndFlush(created);
            newlyRecorded = true;
        } else {
            // managed 엔티티 — 변경 감지로 저장된다. 이미 켜진 종류면 false → 발행 안 함 (멱등).
            newlyRecorded = participation.mark(type);
        }

        if (newlyRecorded) {
            publisher.publishEvent(new AttendanceRecorded(elderId, date, type));
        }
    }
}
