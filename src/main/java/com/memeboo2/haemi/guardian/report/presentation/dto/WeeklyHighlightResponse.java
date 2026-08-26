package com.memeboo2.haemi.guardian.report.presentation.dto;

import com.memeboo2.haemi.guardian.report.application.GetWeeklyHighlightUseCase.WeeklyHighlight;

import java.util.List;
import java.util.UUID;

/** RPT-ATT-005 보호자용 이번 주 하이라이트 응답. */
public record WeeklyHighlightResponse(UUID elderId, List<String> lines) {

    public static WeeklyHighlightResponse from(WeeklyHighlight highlight) {
        return new WeeklyHighlightResponse(highlight.elderId(), highlight.lines());
    }
}
