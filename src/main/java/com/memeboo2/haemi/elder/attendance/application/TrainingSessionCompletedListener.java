package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingSessionCompletedListener {

    private final DailyParticipationRepository repository;
    private final ApplicationEventPublisher publisher;

    /** 같은 (어르신, 날짜) 이벤트가 재전달돼도 한 번만 기록한다 (멱등). */
    @ApplicationModuleListener
    public void on(TrainingSessionCompleted event) {
        if (repository.existsByElderIdAndParticipationDate(event.elderId(), event.sessionDate())) {
            return;
        }
        try {
            repository.saveAndFlush(DailyParticipation.of(event.elderId(), event.sessionDate()));
        } catch (DataIntegrityViolationException alreadyRecorded) {
            return;
        }
        publisher.publishEvent(new AttendanceRecorded(event.elderId(), event.sessionDate()));
    }
}
