package com.memeboo2.haemi.guardian.report.listener;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendanceRecordedListener {

    private final ReportParticipationRepository repository;

    /**
     * 활동 종류 플래그를 멱등하게 미러링한다. 중복 수신에도 안전하다.
     */
    @ApplicationModuleListener
    public void on(AttendanceRecorded event) {
        ActivityType type = event.activityType();
        repository.upsertActivity(event.elderId(), event.participationDate(),
                type == ActivityType.TRAINING,
                type == ActivityType.GREETING_READ,
                type == ActivityType.MEMORY_VIEWED,
                type == ActivityType.REPLIED);
    }
}
