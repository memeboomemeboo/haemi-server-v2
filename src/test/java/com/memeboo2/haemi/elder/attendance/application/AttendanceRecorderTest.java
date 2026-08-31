package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceRecorderTest {

    @Mock DailyParticipationRepository repository;
    @Mock DailyParticipationWriter participationWriter;
    @Mock ApplicationEventPublisher publisher;
    @InjectMocks AttendanceRecorder recorder;

    UUID elderId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 8, 25);

    @Test
    void 종류가_새로_켜지면_해당_종류로_AttendanceRecorded를_발행한다() {
        // 기존 행이 있어 부분 UPDATE가 바로 플래그를 켠다 (affected=1).
        given(repository.markActivity(elderId, date, false, false, false, true)).willReturn(1);

        recorder.record(elderId, date, ActivityType.REPLIED);

        verify(participationWriter, never()).insertIfAbsent(any(), any(), any());
        ArgumentCaptor<AttendanceRecorded> captor = ArgumentCaptor.forClass(AttendanceRecorded.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().elderId()).isEqualTo(elderId);
        assertThat(captor.getValue().participationDate()).isEqualTo(date);
        assertThat(captor.getValue().activityType()).isEqualTo(ActivityType.REPLIED);
    }

    @Test
    void 행이_없으면_멱등_삽입_후_켜고_발행한다() {
        // 첫 UPDATE는 행이 없어 0, 행도 없으므로 삽입 후 재시도는 1.
        given(repository.markActivity(elderId, date, true, false, false, false)).willReturn(0, 1);
        given(repository.existsByElderIdAndParticipationDate(elderId, date)).willReturn(false);

        recorder.record(elderId, date, ActivityType.TRAINING);

        verify(participationWriter).insertIfAbsent(any(UUID.class), eq(elderId), eq(date));
        verify(publisher).publishEvent(any(AttendanceRecorded.class));
    }

    @Test
    void 행이_있고_이미_켜진_종류면_삽입도_발행도_하지_않는다() {
        // 행은 있으나 해당 플래그가 이미 켜져 변화 없음(0). 행이 존재하므로 불필요한 삽입·재시도를 하지 않는다 (#141).
        given(repository.markActivity(elderId, date, true, false, false, false)).willReturn(0);
        given(repository.existsByElderIdAndParticipationDate(elderId, date)).willReturn(true);

        recorder.record(elderId, date, ActivityType.TRAINING);

        verify(participationWriter, never()).insertIfAbsent(any(), any(), any());
        verify(publisher, never()).publishEvent(any());
    }
}
