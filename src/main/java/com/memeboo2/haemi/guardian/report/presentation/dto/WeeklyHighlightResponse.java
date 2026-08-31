package com.memeboo2.haemi.guardian.report.presentation.dto;

import com.memeboo2.haemi.guardian.report.application.GetWeeklyHighlightUseCase.WeeklyHighlight;
import com.memeboo2.haemi.guardian.report.application.WeeklyHighlightItem;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/** RPT-ATT-005 보호자용 이번 주 하이라이트 응답. */
public record WeeklyHighlightResponse(UUID elderId, List<Item> items) {

    @Schema(name = "HighlightItem")
    public record Item(UUID id, String title, String body) {
        static Item from(WeeklyHighlightItem item) {
            return new Item(item.id(), item.title(), item.body());
        }
    }

    public static WeeklyHighlightResponse from(WeeklyHighlight highlight) {
        return new WeeklyHighlightResponse(highlight.elderId(),
                highlight.items().stream().map(Item::from).toList());
    }
}
