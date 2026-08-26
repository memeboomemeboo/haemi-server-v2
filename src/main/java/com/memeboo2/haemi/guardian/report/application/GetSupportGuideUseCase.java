package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.report.api.CognitiveArea;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatus;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery;
import com.memeboo2.haemi.guardian.report.api.CognitiveStatusQuery.AreaStatus;
import com.memeboo2.haemi.guardian.report.infrastructure.ReportParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** RPT-ATT-006: 리포트 상태를 보호자가 실행할 수 있는 행동으로 번역한다. */
@Service
@RequiredArgsConstructor
public class GetSupportGuideUseCase {

    private final CareAccessQuery careAccessQuery;
    private final ElderRepository elderRepository;
    private final ReportParticipationRepository participationRepository;
    private final ReportProperties reportProperties;
    private final CognitiveStatusQuery cognitiveStatusQuery;
    private final HaemiClock clock;

    public record Suggestion(SupportGuideAction action, String message) {}

    public record SupportGuide(String elderName, List<Suggestion> suggestions) {}

    @Transactional(readOnly = true)
    public SupportGuide execute(UUID guardianId, UUID elderId) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);

        Elder elder = elderRepository.findById(elderId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));
        LocalDate today = clock.today();
        LocalDate weekStart = today.minusDays(reportProperties.weeklyWindowDays() - 1L);
        int weeklyParticipationDays = (int) participationRepository
                .findByElderIdAndParticipationDateGreaterThanEqual(elderId, weekStart)
                .stream()
                .filter(participation -> !participation.getParticipationDate().isAfter(today))
                .count();
        CognitiveStatusQuery.CognitiveStatusView cognitiveStatus = cognitiveStatusQuery
                .cognitiveStatus(guardianId, elderId);

        List<Suggestion> suggestions = new ArrayList<>();
        if (weeklyParticipationDays <= 2) {
            suggestions.add(new Suggestion(
                    SupportGuideAction.SEND_DAILY_CARE,
                    "%s 어르신께 하루 한마디를 보내기로 응원해보세요.".formatted(elder.getName())
            ));
        }
        if (hasStatus(cognitiveStatus.areas(), CognitiveArea.RECALL, CognitiveStatus.WATCH)) {
            suggestions.add(new Suggestion(
                    SupportGuideAction.REGISTER_MEMORY,
                    "%s 어르신을 위해 추억 앨범에 가족 사진을 올려보세요.".formatted(elder.getName())
            ));
        }
        if (hasFourWeekDecline(cognitiveStatus.areas(), CognitiveArea.DELAYED_RECALL)) {
            suggestions.add(new Suggestion(
                    SupportGuideAction.CALL_ELDER,
                    "%s 어르신께 안부 전화로 오늘 있었던 일을 여쭤보세요.".formatted(elder.getName())
            ));
        }
        if (allAreasGood(cognitiveStatus.areas())) {
            suggestions.add(new Suggestion(
                    SupportGuideAction.PRAISE_ELDER,
                    "%s 어르신께 칭찬 한마디를 전해보세요.".formatted(elder.getName())
            ));
        }
        return new SupportGuide(elder.getName(), List.copyOf(suggestions));
    }

    private boolean hasStatus(List<AreaStatus> areas, CognitiveArea targetArea, CognitiveStatus targetStatus) {
        return areas.stream().anyMatch(area -> area.area() == targetArea && area.status() == targetStatus);
    }

    private boolean hasFourWeekDecline(List<AreaStatus> areas, CognitiveArea targetArea) {
        return areas.stream().anyMatch(area -> area.area() == targetArea && area.fourWeekDecline());
    }

    private boolean allAreasGood(List<AreaStatus> areas) {
        return Arrays.stream(CognitiveArea.values())
                .allMatch(area -> hasStatus(areas, area, CognitiveStatus.GOOD));
    }
}
