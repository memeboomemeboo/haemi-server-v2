package com.memeboo2.haemi.guardian.report.listener;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.common.persistence.UuidGenerator;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceRecordedListener {

    private final ReportParticipationRepository repository;
    private final ReportParticipationWriter participationWriter;

    /**
     * 활동 종류 플래그를 스냅샷에 원자적으로 미러링한다. 중복 수신·동시 수신에도 안전하다.
     */
    @ApplicationModuleListener
    public void on(AttendanceRecorded event) {
        ActivityType type = event.activityType();
        boolean training = type == ActivityType.TRAINING;
        boolean greetingRead = type == ActivityType.GREETING_READ;
        boolean memoryViewed = type == ActivityType.MEMORY_VIEWED;
        boolean replied = type == ActivityType.REPLIED;

        int updated = repository.markActivity(event.elderId(), event.participationDate(),
                training, greetingRead, memoryViewed, replied);
        if (updated == 0) {
            participationWriter.insertIfAbsent(UuidGenerator.generate(), event.elderId(), event.participationDate());
            repository.markActivity(event.elderId(), event.participationDate(),
                    training, greetingRead, memoryViewed, replied);
        }
    }
}
