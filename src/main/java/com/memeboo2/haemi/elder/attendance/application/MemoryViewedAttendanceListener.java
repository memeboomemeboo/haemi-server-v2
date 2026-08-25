package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.attendance.ActivityType;
import com.memeboo2.haemi.common.event.MemoryViewed;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** 어르신이 추억을 처음 열어보면 그날의 MEMORY_VIEWED 참여로 기록한다. */
@Component
@RequiredArgsConstructor
public class MemoryViewedAttendanceListener {

    private final AttendanceRecorder recorder;

    @ApplicationModuleListener
    public void on(MemoryViewed event) {
        recorder.record(event.elderId(), event.viewedDate(), ActivityType.MEMORY_VIEWED);
    }
}
