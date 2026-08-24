package com.memeboo2.haemi.guardian.report.listener;

import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceRecordedListener {

    private final ReportParticipationRepository repository;

    @ApplicationModuleListener
    public void on(AttendanceRecorded event) {
        if (repository.existsByElderIdAndParticipationDate(event.elderId(), event.participationDate())) {
            return;
        }
        try {
            repository.saveAndFlush(ReportParticipation.of(event.elderId(), event.participationDate()));
        } catch (DataIntegrityViolationException alreadyRecorded) {
            // 멱등 — 이미 적재됨
        }
    }
}
