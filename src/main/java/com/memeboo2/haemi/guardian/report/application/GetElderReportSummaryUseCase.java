package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.common.attendance.StreakCalculator;
import com.memeboo2.haemi.common.attendance.DaysTogetherCalculator;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** RPT-LST-002: 선택한 어르신 요약 카드. */
@Service
@RequiredArgsConstructor
public class GetElderReportSummaryUseCase {


    private final CareAccessQuery careAccessQuery;
    private final ElderRepository elderRepository;
    private final ReportParticipationRepository participationRepository;
    private final ReportStatusCalculator statusCalculator;
    private final ReportProperties props;
    private final HaemiClock clock;

    public record Summary(
            UUID elderId,
            String name,
            Integer age,
            String generation,
            long daysTogether,
            boolean attendedToday,
            int weeklyParticipationDays,
            int weeklyGoalDays,
            ReportStatus status,
            int currentStreak,
            int bestStreak
    ) {}

    @Transactional(readOnly = true)
    public Summary execute(UUID guardianId, UUID elderId) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);
        Elder elder = elderRepository.findById(elderId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));

        LocalDate today = clock.today();
        Integer age = elder.getBirthDate() == null ? null : Period.between(elder.getBirthDate(), today).getYears();
        String generation = age == null ? null : (age / 10 * 10) + "대";
        long daysTogether = DaysTogetherCalculator.daysTogether(elder.getCreatedAt(), today);

        Set<LocalDate> dates = participationRepository.findByElderId(elderId).stream()
                .map(ReportParticipation::getParticipationDate)
                .collect(Collectors.toSet());
        boolean attendedToday = dates.contains(today);
        LocalDate weekStart = today.minusDays(props.weeklyWindowDays() - 1L);
        int weeklyDays = (int) dates.stream().filter(d -> !d.isBefore(weekStart) && !d.isAfter(today)).count();
        int currentStreak = StreakCalculator.currentStreak(dates, today);
        int bestStreak = StreakCalculator.bestStreak(dates);

        return new Summary(
                elderId, elder.getName(), age, generation, daysTogether, attendedToday,
                weeklyDays, props.weeklyWindowDays(),
                statusCalculator.fromWeeklyParticipationDays(weeklyDays),
                currentStreak, bestStreak
        );
    }
}
