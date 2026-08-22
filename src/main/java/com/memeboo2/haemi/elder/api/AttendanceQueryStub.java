package com.memeboo2.haemi.elder.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** elder/attendance 실구현 전까지 사용하는 빈 구현체 (김연호 4단계에서 대체). */
@Component
@ConditionalOnMissingBean(name = "attendanceQueryImpl")
public class AttendanceQueryStub implements AttendanceQuery {

    @Override
    public boolean completedToday(UUID elderId) {
        return false;
    }

    @Override
    public int currentStreak(UUID elderId) {
        return 0;
    }

    @Override
    public long daysTogether(UUID elderId) {
        return 0L;
    }
}
