package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.GuardianRole;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** RPT-LST-001: 가족에 속한 어르신 카드 목록 (🟠 → 🟡 → 🟢 순 정렬, 어르신 간 비교·순위는 없음). */
@Service
@RequiredArgsConstructor
public class GetElderReportListUseCase {

    private static final Comparator<Card> BY_STATUS_PRIORITY = Comparator.comparingInt(c -> switch (c.status()) {
        case WATCH -> 0;
        case NORMAL -> 1;
        case GOOD -> 2;
    });

    private final CareAccessQuery careAccessQuery;
    private final ElderRepository elderRepository;
    private final ReportParticipationRepository participationRepository;
    private final ReportStatusCalculator statusCalculator;
    private final ReportProperties props;
    private final HaemiClock clock;

    public record Card(
            UUID elderId,
            String name,
            GuardianRole role,
            Integer age,
            boolean attendedToday,
            ReportStatus status
    ) {}

    @Transactional(readOnly = true)
    public List<Card> execute(UUID guardianId) {
        LocalDate today = clock.today();
        LocalDate weekStart = today.minusDays(props.weeklyWindowDays() - 1L);

        List<Card> cards = careAccessQuery.accessibleElders(guardianId).stream()
                .map(elderId -> toCard(guardianId, elderId, today, weekStart))
                .filter(Objects::nonNull)
                .toList();

        return cards.stream().sorted(BY_STATUS_PRIORITY).toList();
    }

    private Card toCard(UUID guardianId, UUID elderId, LocalDate today, LocalDate weekStart) {
        Elder elder = elderRepository.findById(elderId).orElse(null);
        if (elder == null) {
            return null;
        }
        GuardianRole role = careAccessQuery.roleOf(guardianId, elderId);
        Integer age = elder.getBirthDate() == null ? null : Period.between(elder.getBirthDate(), today).getYears();

        Set<LocalDate> dates = participationRepository
                .findByElderIdAndParticipationDateGreaterThanEqual(elderId, weekStart).stream()
                .map(ReportParticipation::getParticipationDate)
                .collect(Collectors.toSet());
        boolean attendedToday = dates.contains(today);
        int weeklyDays = (int) dates.stream().filter(d -> !d.isBefore(weekStart) && !d.isAfter(today)).count();

        return new Card(elderId, elder.getName(), role, age, attendedToday,
                statusCalculator.fromWeeklyParticipationDays(weeklyDays));
    }
}
