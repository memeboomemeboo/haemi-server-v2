package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import com.memeboo2.haemi.guardian.api.AttendanceBadge;
import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import com.memeboo2.haemi.guardian.api.ElderQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** AttendanceQueryStub을 대체하는 실구현. bean name이 "attendanceQueryImpl"이어야 한다. */
@Service("attendanceQueryImpl")
@RequiredArgsConstructor
public class AttendanceQueryImpl implements AttendanceQuery {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int STREAK_DATE_PAGE_SIZE = 31;

    private final DailyParticipationRepository repository;
    private final ElderQuery elderQuery;
    private final HaemiClock clock;

    @Override
    @Transactional(readOnly = true)
    public boolean completedToday(UUID elderId) {
        return repository.existsByElderIdAndParticipationDate(elderId, clock.today());
    }

    @Override
    @Transactional(readOnly = true)
    public int currentStreak(UUID elderId) {
        LocalDate today = clock.today();
        // 오늘 미참여면 스트릭은 0 — 이력을 읽지 않고 즉시 반환한다.
        if (!repository.existsByElderIdAndParticipationDate(elderId, today)) {
            return 0;
        }
        // 최신 날짜부터 31일 단위로 훑으며 연속이 끊기는 첫 지점에서 종료한다.
        LocalDate cursor = today;
        int streak = 0;
        int page = 0;
        while (true) {
            List<LocalDate> dates = repository.findParticipationDatesDesc(
                    elderId, PageRequest.of(page++, STREAK_DATE_PAGE_SIZE));
            if (dates.isEmpty()) {
                return streak;
            }
            for (LocalDate date : dates) {
                if (date.isAfter(cursor)) {
                    continue;
                }
                if (!date.equals(cursor)) {
                    return streak;
                }
                streak++;
                cursor = cursor.minusDays(1);
            }
            if (dates.size() < STREAK_DATE_PAGE_SIZE) {
                return streak;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long daysTogether(UUID elderId) {
        return elderQuery.findById(elderId)
                .map(info -> {
                    LocalDate registeredDate = info.registeredAt().atZone(KST).toLocalDate();
                    return ChronoUnit.DAYS.between(registeredDate, clock.today());
                })
                .orElse(0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceBadge> unlockedBadges(UUID elderId) {
        return badgesFor(repository.countByElderId(elderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceBadge> unlockedBadgesAfterCompletion(UUID elderId) {
        long participationDays = repository.countByElderId(elderId);
        if (!repository.existsByElderIdAndParticipationDate(elderId, clock.today())) {
            participationDays++;
        }
        return badgesFor(participationDays);
    }

    private List<AttendanceBadge> badgesFor(long participationDays) {
        return Arrays.stream(AttendanceBadge.values())
                .filter(badge -> badge.isUnlockedBy(participationDays))
                .toList();
    }
}
