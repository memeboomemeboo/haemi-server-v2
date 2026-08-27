package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.report.api.CognitiveArea;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightFact;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightPrompt;
import com.memeboo2.haemi.platform.ai.api.WeeklyHighlightWriter;
import com.memeboo2.haemi.guardian.report.infrastructure.WeeklyHighlightOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** RPT-ATT-005: 이번 주의 잘한 점을 먼저, 관찰 신호를 나중에 전달한다. */
@Service
@RequiredArgsConstructor
public class GetWeeklyHighlightUseCase {

    private final CareAccessQuery careAccessQuery;
    private final CognitiveStatusQuery cognitiveStatusQuery;
    private final WeeklyParticipationDaysCounter weeklyParticipationDaysCounter;
    private final WeeklyHighlightWriter weeklyHighlightWriter;
    private final WeeklyHighlightOverrideRepository overrideRepository;
    private final HaemiClock clock;

    public record WeeklyHighlight(UUID elderId, List<String> lines) {
        public WeeklyHighlight {
            lines = List.copyOf(lines);
        }
    }

    @Transactional(readOnly = true)
    public WeeklyHighlight execute(UUID guardianId, UUID elderId) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);

        LocalDate today = clock.today();

        // 보호자가 이번 주 문구를 편집(#100 M5)했다면 자동 생성 대신 그 문구를 그대로 반환한다.
        LocalDate weekStart = WeekAnchor.of(today);
        var override = overrideRepository.findByElderIdAndWeekStart(elderId, weekStart);
        if (override.isPresent()) {
            return new WeeklyHighlight(elderId, WeekAnchor.splitLines(override.get().getContent()));
        }

        int weeklyParticipationDays = weeklyParticipationDaysCounter.count(elderId, today);

        CognitiveStatusQuery.CognitiveStatusView cognitive = cognitiveStatusQuery.cognitiveStatus(guardianId, elderId);
        WeeklyHighlightPrompt prompt = new WeeklyHighlightPrompt(
                weeklyParticipationDays,
                cognitive.areas().stream()
                        .filter(area -> area.status() == CognitiveStatus.GOOD)
                        .map(area -> strengthFor(area.area()))
                        .toList(),
                cognitive.areas().stream()
                        .filter(area -> area.status() == CognitiveStatus.WATCH || area.fourWeekDecline())
                        .map(area -> supportFor(area.area()))
                        .toList()
        );

        return new WeeklyHighlight(elderId, weeklyHighlightWriter.write(prompt));
    }

    private WeeklyHighlightFact strengthFor(CognitiveArea area) {
        return switch (area) {
            case ORIENTATION -> WeeklyHighlightFact.ORIENTATION_STRENGTH;
            case RECALL -> WeeklyHighlightFact.RECALL_STRENGTH;
            case LANGUAGE -> WeeklyHighlightFact.LANGUAGE_STRENGTH;
            case DELAYED_RECALL -> WeeklyHighlightFact.DELAYED_RECALL_STRENGTH;
        };
    }

    private WeeklyHighlightFact supportFor(CognitiveArea area) {
        return switch (area) {
            case ORIENTATION -> WeeklyHighlightFact.ORIENTATION_SUPPORT;
            case RECALL -> WeeklyHighlightFact.RECALL_SUPPORT;
            case LANGUAGE -> WeeklyHighlightFact.LANGUAGE_SUPPORT;
            case DELAYED_RECALL -> WeeklyHighlightFact.DELAYED_RECALL_SUPPORT;
        };
    }
}
