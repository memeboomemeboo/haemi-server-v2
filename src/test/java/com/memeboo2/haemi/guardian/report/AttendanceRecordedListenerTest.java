package com.memeboo2.haemi.guardian.report;

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

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceRecordedListenerTest {

    @Mock ReportParticipationRepository repository;
    @InjectMocks AttendanceRecordedListener listener;

    UUID elderId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 8, 25);

    @Test
    void 정상_경로_스냅샷을_적재한다() {
        listener.on(new AttendanceRecorded(elderId, date));

        verify(repository).insertIfAbsent(any(UUID.class), eq(elderId), eq(date));
    }

    @Test
    void 이미_적재됐으면_원자적_삽입이_아무것도_하지_않는다_멱등() {
        given(repository.insertIfAbsent(any(UUID.class), eq(elderId), eq(date))).willReturn(0);

        listener.on(new AttendanceRecorded(elderId, date));

        verify(repository).insertIfAbsent(any(UUID.class), eq(elderId), eq(date));
    }
}
