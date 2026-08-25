package com.memeboo2.haemi.elder.attendance;

import com.memeboo2.haemi.common.event.AttendanceRecorded;
import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import com.memeboo2.haemi.elder.attendance.application.TrainingSessionCompletedListener;
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
class TrainingSessionCompletedListenerTest {

    @Mock DailyParticipationWriter participationWriter;
    @Mock ApplicationEventPublisher publisher;
    @InjectMocks TrainingSessionCompletedListener listener;

    UUID elderId = UUID.randomUUID();
    LocalDate sessionDate = LocalDate.of(2026, 8, 25);

    @Test
    void 정상_경로_참여기록_저장후_출석이벤트_발행() {
        given(participationWriter.insertIfAbsent(any(UUID.class), eq(elderId), eq(sessionDate))).willReturn(true);

        listener.on(new TrainingSessionCompleted(elderId, sessionDate));

        ArgumentCaptor<AttendanceRecorded> captor = ArgumentCaptor.forClass(AttendanceRecorded.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().elderId()).isEqualTo(elderId);
        assertThat(captor.getValue().participationDate()).isEqualTo(sessionDate);
    }

    @Test
    void 이미_기록된_날짜면_중복_저장하지_않는다_멱등() {
        given(participationWriter.insertIfAbsent(any(UUID.class), eq(elderId), eq(sessionDate))).willReturn(false);

        listener.on(new TrainingSessionCompleted(elderId, sessionDate));

        verify(publisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }
}
