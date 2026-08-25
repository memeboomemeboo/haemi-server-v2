package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationWriter;
import com.memeboo2.haemi.guardian.report.listener.AttendanceRecordedListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceRecordedListenerTest {

    @Mock ReportParticipationRepository repository;
    @Mock ReportParticipationWriter participationWriter;
    @InjectMocks AttendanceRecordedListener listener;

    UUID elderId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 8, 25);

    @Test
    void 기존_행이면_해당_종류_플래그를_원자적으로_켠다() {
        given(repository.markActivity(elderId, date, false, true, false, false)).willReturn(1);

        listener.on(new AttendanceRecorded(elderId, date, ActivityType.GREETING_READ));

        verify(participationWriter, never()).insertIfAbsent(any(), any(), any());
    }

    @Test
    void 행이_없으면_멱등_삽입_후_켠다() {
        given(repository.markActivity(elderId, date, false, false, true, false)).willReturn(0, 1);

        listener.on(new AttendanceRecorded(elderId, date, ActivityType.MEMORY_VIEWED));

        verify(participationWriter).insertIfAbsent(any(UUID.class), eq(elderId), eq(date));
        verify(repository, org.mockito.Mockito.times(2)).markActivity(elderId, date, false, false, true, false);
    }
}
