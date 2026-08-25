package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import com.memeboo2.haemi.common.persistence.UuidGenerator;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingSessionCompletedListener {

    private final DailyParticipationRepository repository;
    private final ApplicationEventPublisher publisher;

    /**
     * 같은 (어르신, 날짜) 이벤트가 재전달돼도 한 번만 기록한다 (멱등).
     * 표준 MERGE로 원자적으로 삽입한다 — exists 검사 후 saveAndFlush를 하면
     * 두 이벤트가 동시에 exists를 통과했을 때 한쪽의 unique 위반이 REQUIRES_NEW 트랜잭션
     * 전체를 커밋 불가 상태로 만들어, catch로 잡아도 Modulith가 실패로 보고 재시도한다.
     */
    @ApplicationModuleListener
    public void on(TrainingSessionCompleted event) {
        int inserted = repository.insertIfAbsent(UuidGenerator.generate(), event.elderId(), event.sessionDate());
        if (inserted == 0) {
            return;
        }
        publisher.publishEvent(new AttendanceRecorded(event.elderId(), event.sessionDate()));
    }
}
