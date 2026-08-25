package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 완료 이벤트를 멱등적인 일자별 출석으로 투영한다. */
@Component
@RequiredArgsConstructor
public class TrainingAttendanceListener {

    private final DailyParticipationRepository participationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @ApplicationModuleListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(TrainingSessionCompleted event) {
        if (participationRepository.existsByTrainingSessionId(event.trainingSessionId())) {
            return;
        }

        participationRepository.save(DailyParticipation.from(event));
        eventPublisher.publishEvent(new AttendanceRecorded(
                event.trainingSessionId(), event.elderId(), event.completedDate(), event.completedAt()));
    }
}
