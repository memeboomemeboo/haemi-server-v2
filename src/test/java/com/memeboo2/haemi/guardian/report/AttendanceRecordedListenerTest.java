package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import com.memeboo2.haemi.guardian.report.listener.AttendanceRecordedListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceRecordedListenerTest {

    @Mock ReportParticipationRepository repository;
    @InjectMocks AttendanceRecordedListener listener;

    UUID elderId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 8, 25);

    @Test
    void 활동_종류_플래그를_스냅샷에_미러링한다() {
        listener.on(new AttendanceRecorded(elderId, date, ActivityType.GREETING_READ));

        // GREETING_READ만 true로 upsert
        verify(repository).upsertActivity(elderId, date, false, true, false, false);
    }
}
