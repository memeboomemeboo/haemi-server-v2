package com.memeboo2.haemi.guardian.home.application;

import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** attendance 실구현 전까지 홈 화면을 위한 안전한 기본값을 제공한다. */
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

    @Override
    public List<com.memeboo2.haemi.guardian.api.AttendanceBadge> unlockedBadges(UUID elderId) {
        return List.of();
    }

    @Override
    public List<com.memeboo2.haemi.guardian.api.AttendanceBadge> unlockedBadgesAfterCompletion(UUID elderId) {
        return List.of();
    }
}
