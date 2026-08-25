package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.common.attendance.StreakCalculator;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** RPT-ATT-003: 최근 7일 + 최근 4주 출석·참여 현황. */
@Service
@RequiredArgsConstructor
public class GetAttendanceDetailUseCase {

    private final CareAccessQuery careAccessQuery;
    private final ReportParticipationRepository participationRepository;
    private final ReportStatusCalculator statusCalculator;
    private final ReportProperties props;
    private final HaemiClock clock;

    public record DayMark(LocalDate date, DayOfWeek dayOfWeek, boolean participated) {}

    public record WeekBar(LocalDate weekStart, LocalDate weekEnd, int participatedDays) {}

    public record AttendanceDetail(
            List<DayMark> last7Days,
            List<WeekBar> last4Weeks,
            int currentStreak,
            int bestStreak,
            ReportStatus weeklyStatus
    ) {}

    @Transactional(readOnly = true)
    public AttendanceDetail execute(UUID guardianId, UUID elderId) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);
        LocalDate today = clock.today();
        int weeklyWindow = props.weeklyWindowDays();
        int monthlyWeeks = props.monthlyWindowWeeks();
        LocalDate windowStart = today.minusDays((long) monthlyWeeks * 7 - 1);

        Set<LocalDate> recentDates = participationRepository
                .findByElderIdAndParticipationDateGreaterThanEqual(elderId, windowStart).stream()
                .map(ReportParticipation::getParticipationDate)
                .collect(Collectors.toSet());

        List<DayMark> last7Days = IntStream.range(0, weeklyWindow)
                .mapToObj(i -> today.minusDays(weeklyWindow - 1L - i))
                .map(d -> new DayMark(d, d.getDayOfWeek(), recentDates.contains(d)))
                .toList();

        List<WeekBar> last4Weeks = IntStream.range(0, monthlyWeeks)
                .mapToObj(w -> {
                    LocalDate weekStart = today.minusDays((long) (monthlyWeeks - w) * 7 - 1);
                    LocalDate weekEnd = weekStart.plusDays(6);
                    int count = (int) recentDates.stream()
                            .filter(d -> !d.isBefore(weekStart) && !d.isAfter(weekEnd)).count();
                    return new WeekBar(weekStart, weekEnd, count);
                })
                .toList();

        Set<LocalDate> allDates = participationRepository.findByElderId(elderId).stream()
                .map(ReportParticipation::getParticipationDate)
                .collect(Collectors.toSet());
        int currentStreak = StreakCalculator.currentStreak(allDates, today);
        int bestStreak = StreakCalculator.bestStreak(allDates);

        LocalDate weekStart = today.minusDays(weeklyWindow - 1L);
        int weeklyDays = (int) recentDates.stream().filter(d -> !d.isBefore(weekStart)).count();

        return new AttendanceDetail(last7Days, last4Weeks, currentStreak, bestStreak,
                statusCalculator.fromWeeklyParticipationDays(weeklyDays));
    }
}
