package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import com.memeboo2.haemi.common.persistence.UuidGenerator;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingSessionCompletedListener {

    private final DailyParticipationWriter participationWriter;
    private final ApplicationEventPublisher publisher;

    /**
     * 같은 (어르신, 날짜) 이벤트가 재전달돼도 한 번만 기록한다 (멱등).
     * PostgreSQL의 ON CONFLICT 경로는 unique 위반 없이 중복 삽입을 무시한다.
     */
    @ApplicationModuleListener
    public void on(TrainingSessionCompleted event) {
        if (!participationWriter.insertIfAbsent(UuidGenerator.generate(), event.elderId(), event.sessionDate())) {
            return;
        }
        publisher.publishEvent(new AttendanceRecorded(event.elderId(), event.sessionDate()));
    }
}
