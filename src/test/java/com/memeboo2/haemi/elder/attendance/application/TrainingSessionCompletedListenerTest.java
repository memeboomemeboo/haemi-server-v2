package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainingSessionCompletedListenerTest {

    @Mock AttendanceRecorder recorder;
    @InjectMocks TrainingSessionCompletedListener listener;

    UUID elderId = UUID.randomUUID();
    LocalDate sessionDate = LocalDate.of(2026, 8, 25);

    @Test
    void 훈련_완료를_TRAINING_활동으로_기록한다() {
        listener.on(new TrainingSessionCompleted(elderId, sessionDate));

        verify(recorder).record(elderId, sessionDate, ActivityType.TRAINING);
    }
}
