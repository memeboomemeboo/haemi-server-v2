package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.guardian.report.domain.ReportParticipation;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/** 주간 리포트에서 사용하는 고유 참여일 수를 일관되게 계산한다. */
@Component
@RequiredArgsConstructor
public class WeeklyParticipationDaysCounter {

    private final ReportParticipationRepository participationRepository;
    private final ReportProperties reportProperties;

    public int count(UUID elderId, LocalDate today) {
        LocalDate weekStart = today.minusDays(reportProperties.weeklyWindowDays() - 1L);
        return (int) participationRepository
                .findByElderIdAndParticipationDateGreaterThanEqual(elderId, weekStart)
                .stream()
                .map(ReportParticipation::getParticipationDate)
                .filter(date -> !date.isAfter(today))
                .distinct()
                .count();
    }
}
