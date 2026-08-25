package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceRecorderTest {

    @Mock DailyParticipationRepository repository;
    @Mock ApplicationEventPublisher publisher;
    @InjectMocks AttendanceRecorder recorder;

    UUID elderId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 8, 25);

    @Test
    void 종류가_새로_기록되면_해당_종류로_AttendanceRecorded를_발행한다() {
        given(repository.findByElderIdAndParticipationDate(elderId, date)).willReturn(Optional.empty());

        recorder.record(elderId, date, ActivityType.REPLIED);

        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any(DailyParticipation.class));
        ArgumentCaptor<AttendanceRecorded> captor = ArgumentCaptor.forClass(AttendanceRecorded.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().elderId()).isEqualTo(elderId);
        assertThat(captor.getValue().participationDate()).isEqualTo(date);
        assertThat(captor.getValue().activityType()).isEqualTo(ActivityType.REPLIED);
    }

    @Test
    void 이미_기록된_종류면_발행하지_않는다_멱등() {
        DailyParticipation existing = DailyParticipation.of(elderId, date);
        existing.mark(ActivityType.TRAINING); // 이미 TRAINING이 켜진 상태
        given(repository.findByElderIdAndParticipationDate(elderId, date)).willReturn(Optional.of(existing));

        recorder.record(elderId, date, ActivityType.TRAINING);

        verify(publisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }
}
