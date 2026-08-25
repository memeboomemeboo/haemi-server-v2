package com.memeboo2.haemi.guardian.report;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import com.memeboo2.haemi.guardian.report.listener.AttendanceRecordedListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceRecordedListenerTest {

    @Mock ReportParticipationRepository repository;
    @InjectMocks AttendanceRecordedListener listener;

    UUID elderId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 8, 25);

    @Test
    void 신규_날짜면_해당_종류_플래그로_스냅샷을_생성한다() {
        given(repository.findByElderIdAndParticipationDate(elderId, date)).willReturn(Optional.empty());

        listener.on(new AttendanceRecorded(elderId, date, ActivityType.GREETING_READ));

        ArgumentCaptor<ReportParticipation> captor = ArgumentCaptor.forClass(ReportParticipation.class);
        verify(repository).saveAndFlush(captor.capture());
        ReportParticipation saved = captor.getValue();
        assertThat(saved.isGreetingReadDone()).isTrue();
        assertThat(saved.isTrainingDone()).isFalse();
    }

    @Test
    void 기존_스냅샷이면_해당_종류_플래그를_켠다() {
        ReportParticipation existing = ReportParticipation.of(elderId, date);
        given(repository.findByElderIdAndParticipationDate(elderId, date)).willReturn(Optional.of(existing));

        listener.on(new AttendanceRecorded(elderId, date, ActivityType.MEMORY_VIEWED));

        assertThat(existing.isMemoryViewedDone()).isTrue();
    }
}
