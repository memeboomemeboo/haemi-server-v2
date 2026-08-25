package com.memeboo2.haemi.elder.attendance.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.attendance.domain.DailyParticipation;
import com.memeboo2.haemi.elder.attendance.infrastructure.DailyParticipationRepository;
import com.memeboo2.haemi.guardian.api.AttendanceQuery;
import com.memeboo2.haemi.guardian.api.ElderQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** AttendanceQueryStub을 대체하는 실구현. bean name이 "attendanceQueryImpl"이어야 한다. */
@Service("attendanceQueryImpl")
@RequiredArgsConstructor
public class AttendanceQueryImpl implements AttendanceQuery {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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
        // 최신 날짜부터 훑으며 연속이 끊기는 첫 지점에서 종료한다 (스트릭 길이만큼만 소비).
        LocalDate cursor = today;
        int streak = 0;
        for (LocalDate date : repository.findParticipationDatesDesc(elderId)) {
            if (date.isAfter(cursor)) {
                continue;
            }
            if (!date.equals(cursor)) {
                break;
            }
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private static final int WEEKLY_WINDOW_DAYS = 7;

    @Override
    @Transactional(readOnly = true)
    public List<DayActivity> weeklyActivities(UUID elderId) {
        LocalDate today = clock.today();
        LocalDate windowStart = today.minusDays(WEEKLY_WINDOW_DAYS - 1L);
        Map<LocalDate, DailyParticipation> byDate = repository
                .findByElderIdAndParticipationDateGreaterThanEqual(elderId, windowStart).stream()
                .collect(Collectors.toMap(DailyParticipation::getParticipationDate, Function.identity(), (a, b) -> a));

        return IntStream.range(0, WEEKLY_WINDOW_DAYS)
                .mapToObj(i -> today.minusDays(WEEKLY_WINDOW_DAYS - 1L - i))
                .map(d -> {
                    DailyParticipation p = byDate.get(d);
                    if (p == null) {
                        return new DayActivity(d, d.getDayOfWeek(), false, false, false, false);
                    }
                    return new DayActivity(d, d.getDayOfWeek(),
                            p.isTrainingDone(), p.isGreetingReadDone(), p.isMemoryViewedDone(), p.isRepliedDone());
                })
                .toList();
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
}
