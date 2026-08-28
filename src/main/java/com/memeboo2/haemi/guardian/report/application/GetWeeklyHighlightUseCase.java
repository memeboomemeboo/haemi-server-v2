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

    public record WeeklyHighlight(UUID elderId, List<WeeklyHighlightItem> items) {
        public WeeklyHighlight {
            items = List.copyOf(items);
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
            return new WeeklyHighlight(elderId,
                    WeeklyHighlightItemCodec.decode(override.get().getContent(), elderId, weekStart));
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

        // 아직 덮어쓰기 행이 없는 자동 문구도 PATCH의 item.id로 다시 보낼 수 있어야 한다.
        // 따라서 조회마다 새 UUID를 만들지 않고 (elder, week, item index)로 안정적인 ID를 만든다.
        List<WeeklyHighlightItem> items = WeeklyHighlightItemCodec.generatedItems(
                elderId, weekStart, weeklyHighlightWriter.write(prompt));
        return new WeeklyHighlight(elderId, items);
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
