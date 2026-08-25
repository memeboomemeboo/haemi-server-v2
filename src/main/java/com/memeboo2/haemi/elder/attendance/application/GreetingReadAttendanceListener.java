package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.GreetingRead;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** 어르신이 하루 한마디를 읽으면 그날의 GREETING_READ 참여로 기록한다. */
@Component
@RequiredArgsConstructor
public class GreetingReadAttendanceListener {

    private final AttendanceRecorder recorder;

    @ApplicationModuleListener
    public void on(GreetingRead event) {
        recorder.record(event.elderId(), event.readDate(), ActivityType.GREETING_READ);
    }
}
