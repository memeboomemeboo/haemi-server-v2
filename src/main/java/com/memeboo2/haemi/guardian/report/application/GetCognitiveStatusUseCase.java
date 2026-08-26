package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.report.api.CognitiveArea;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery;
import com.memeboo2.haemi.guardian.report.domain.CognitiveResultSnapshot;
import com.memeboo2.haemi.guardian.report.infrastructure.CognitiveResultSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** RPT-ATT-004: 이벤트 스냅샷으로 계산하는 인지 영역별 상태 조회. */
@Service
@RequiredArgsConstructor
public class GetCognitiveStatusUseCase implements CognitiveStatusQuery {

    private final CareAccessQuery careAccessQuery;
    private final CognitiveResultSnapshotRepository snapshotRepository;
    private final CognitiveStatusCalculator calculator;
    private final ReportProperties properties;
    private final HaemiClock clock;

    @Override
    @Transactional(readOnly = true)
    public CognitiveStatusView cognitiveStatus(UUID guardianId, UUID elderId) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);
        LocalDate today = clock.today();
        int trendWeeks = properties.cognitiveTrendWindowWeeks();
        LocalDate trendStart = today.minusDays((long) trendWeeks * 7 - 1);
        List<CognitiveResultSnapshot> snapshots = snapshotRepository
                .findByElderIdAndSessionDateGreaterThanEqual(elderId, trendStart);
        Map<String, List<CognitiveResultSnapshot>> snapshotsByArea = snapshots.stream()
                .collect(Collectors.groupingBy(CognitiveResultSnapshot::getCognitiveArea));
        LocalDate recentStart = today.minusDays(properties.cognitiveRecentWindowDays() - 1L);
        List<AreaStatus> areas = Arrays.stream(CognitiveArea.values())
                .map(area -> areaStatus(area, snapshotsByArea.getOrDefault(area.name(), List.of()), recentStart, today))
                .toList();
        return new CognitiveStatusView(elderId, areas);
    }

    private AreaStatus areaStatus(
            CognitiveArea area,
            List<CognitiveResultSnapshot> areaSnapshots,
            LocalDate recentStart,
            LocalDate today
    ) {
        int recentScored = areaSnapshots.stream()
                .filter(snapshot -> !snapshot.getSessionDate().isBefore(recentStart))
                .mapToInt(CognitiveResultSnapshot::getScoredAnswerCount)
                .sum();
        int recentCorrect = areaSnapshots.stream()
                .filter(snapshot -> !snapshot.getSessionDate().isBefore(recentStart))
                .mapToInt(CognitiveResultSnapshot::getCorrectAnswerCount)
                .sum();
        boolean fourWeekDecline = hasFourWeekDecline(areaSnapshots, today);
        CognitiveStatus status = calculator.status(recentScored, recentCorrect, fourWeekDecline);
        return new AreaStatus(area, status, fourWeekDecline);
    }

    private boolean hasFourWeekDecline(List<CognitiveResultSnapshot> snapshots, LocalDate today) {
        int weeks = properties.cognitiveTrendWindowWeeks();
        int[] scoredCounts = new int[weeks];
        int[] correctCounts = new int[weeks];
        LocalDate start = today.minusDays((long) weeks * 7 - 1);

        for (CognitiveResultSnapshot snapshot : snapshots) {
            if (snapshot.getSessionDate().isBefore(start) || snapshot.getSessionDate().isAfter(today)) {
                continue;
            }
            long offset = ChronoUnit.DAYS.between(start, snapshot.getSessionDate());
            int weekIndex = (int) (offset / 7);
            scoredCounts[weekIndex] += snapshot.getScoredAnswerCount();
            correctCounts[weekIndex] += snapshot.getCorrectAnswerCount();
        }
        return calculator.strictlyDeclines(scoredCounts, correctCounts);
    }
}
