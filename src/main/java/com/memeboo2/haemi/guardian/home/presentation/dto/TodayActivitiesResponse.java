package com.memeboo2.haemi.guardian.home.presentation.dto;

import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase.ActivityEntry;
import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 보호자 홈 "오늘의 기록" 타임라인 응답 (#100 M2). */
public record TodayActivitiesResponse(LocalDate date, List<Item> items) {

    public record Item(
            @Schema(description = "활동 발생 시각") Instant occurredAt,
            @Schema(description = "활동 종류") ActivityType type,
            @Schema(description = "타임라인 제목") String title,
            @Schema(description = "활동별 상세 정보") Map<String, Object> detail
    ) {}

    public static TodayActivitiesResponse from(LocalDate date, List<ActivityEntry> entries) {
        return new TodayActivitiesResponse(date, entries.stream()
                .map(e -> new Item(e.occurredAt(), e.type(), e.title(), e.detail()))
                .toList());
    }
}
