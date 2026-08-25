package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.ElderResponded;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** 어르신이 추억에 응답하면 그날의 REPLIED 참여로 기록한다. */
@Component
@RequiredArgsConstructor
public class ElderRespondedAttendanceListener {

    private final AttendanceRecorder recorder;

    @ApplicationModuleListener
    public void on(ElderResponded event) {
        recorder.record(event.elderId(), event.respondedDate(), ActivityType.REPLIED);
    }
}
