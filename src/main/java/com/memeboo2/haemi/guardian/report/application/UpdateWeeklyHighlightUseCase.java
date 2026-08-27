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
    public WeeklyHighlight executeItems(UUID guardianId, UUID elderId, List<WeeklyHighlightItem> items) {
        careAccessQuery.requireGuardianOf(guardianId, elderId);

        if (items == null || items.isEmpty() || items.stream().anyMatch(item -> item == null
                || item.title() == null || item.title().isBlank() || item.body() == null || item.body().isBlank()
                || containsSeparator(item.title()) || containsSeparator(item.body()))) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "하이라이트 문구를 한 줄 이상 입력해주세요.");
        }
        items = items.stream().map(item -> item.id() == null
                ? new WeeklyHighlightItem(UUID.randomUUID(), item.title(), item.body())
                : item).toList();
        String content = WeeklyHighlightItemCodec.encode(items);
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

        return new WeeklyHighlight(elderId, WeeklyHighlightItemCodec.decode(override.getContent()));
    }

    private boolean containsSeparator(String value) {
        return value.indexOf('\u001e') >= 0 || value.indexOf('\u001f') >= 0;
    }
}
