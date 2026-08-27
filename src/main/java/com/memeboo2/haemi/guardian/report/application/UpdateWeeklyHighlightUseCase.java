package com.memeboo2.haemi.guardian.report.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.report.application.GetWeeklyHighlightUseCase.WeeklyHighlight;
import com.memeboo2.haemi.guardian.report.domain.WeeklyHighlightOverride;
import com.memeboo2.haemi.guardian.report.infrastructure.WeeklyHighlightOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 보호자가 "이번 주 하이라이트" 문구를 편집한다 (#100 M5). */
@Service
@RequiredArgsConstructor
public class UpdateWeeklyHighlightUseCase {

    private static final int MAX_CONTENT_LENGTH = 2000;

    private final CareAccessQuery careAccessQuery;
    private final WeeklyHighlightOverrideRepository overrideRepository;
    private final HaemiClock clock;

    @Transactional
    public WeeklyHighlight execute(UUID guardianId, UUID elderId, List<String> lines) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);

        if (lines == null || lines.isEmpty() || lines.stream().allMatch(l -> l == null || l.isBlank())) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "하이라이트 문구를 한 줄 이상 입력해주세요.");
        }
        String content = WeekAnchor.joinLines(lines);
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "하이라이트 문구가 너무 깁니다.");
        }

        LocalDate weekStart = WeekAnchor.of(clock.today());
        WeeklyHighlightOverride override = overrideRepository
                .findByElderIdAndWeekStart(elderId, weekStart)
                .map(existing -> {
                    existing.updateContent(content);
                    return existing;
                })
                .orElseGet(() -> overrideRepository.save(
                        WeeklyHighlightOverride.of(elderId, weekStart, content)));

        return new WeeklyHighlight(elderId, WeekAnchor.splitLines(override.getContent()));
    }
}
