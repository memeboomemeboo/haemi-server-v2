package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingSessionCompletedListener {

    private final AttendanceRecorder recorder;

    /** 같은 (어르신, 날짜) 이벤트가 재전달돼도 TRAINING을 한 번만 기록한다 (멱등). */
    @ApplicationModuleListener
    public void on(TrainingSessionCompleted event) {
        recorder.record(event.elderId(), event.sessionDate(), ActivityType.TRAINING);
    }
}
