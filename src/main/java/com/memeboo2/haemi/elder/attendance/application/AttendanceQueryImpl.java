package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import com.memeboo2.haemi.guardian.api.AttendanceBadge;
import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import com.memeboo2.haemi.guardian.api.ElderProfileQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 어르신·보호자 홈이 공유하는 출석 읽기 모델의 실구현이다. */
@Component("attendanceQueryImpl")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceQueryImpl implements AttendanceQuery {

    private final DailyParticipationRepository participationRepository;
    private final ElderProfileQuery elderProfileQuery;
    private final HaemiClock clock;

    @Override
    public boolean completedToday(UUID elderId) {
        return participationRepository.existsByElderIdAndParticipationDate(elderId, clock.today());
    }

    @Override
    public int currentStreak(UUID elderId) {
        LocalDate today = clock.today();
        Set<LocalDate> participationDates = new HashSet<>(
                participationRepository.findParticipationDatesThrough(elderId, today));
        int streak = 0;
        for (LocalDate date = today; participationDates.contains(date); date = date.minusDays(1)) {
            streak++;
        }
        return streak;
    }

    @Override
    public long daysTogether(UUID elderId) {
        LocalDate registeredDate = HaemiClock.dateInKst(elderProfileQuery.findById(elderId).registeredAt());
        return Math.max(0, ChronoUnit.DAYS.between(registeredDate, clock.today()));
    }

    @Override
    public List<AttendanceBadge> unlockedBadges(UUID elderId) {
        return badgesFor(participationRepository.countByElderId(elderId));
    }

    @Override
    public List<AttendanceBadge> unlockedBadgesAfterCompletion(UUID elderId, UUID trainingSessionId) {
        long participationDays = participationRepository.countByElderId(elderId);
        if (!participationRepository.existsByTrainingSessionId(trainingSessionId)) {
            participationDays++;
        }
        return badgesFor(participationDays);
    }

    private List<AttendanceBadge> badgesFor(long participationDays) {
        return java.util.Arrays.stream(AttendanceBadge.values())
                .filter(badge -> badge.isUnlockedBy(participationDays))
                .toList();
    }
}
