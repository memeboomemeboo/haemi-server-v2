package com.memeboo2.haemi.guardian.home.presentation.dto;

import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase.ActivityEntry;
import com.memeboo2.haemi.guardian.home.application.GetTodayActivitiesUseCase.ActivityKind;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 보호자 홈 "오늘의 기록" 타임라인 응답 (#100 M2). */
public record TodayActivitiesResponse(List<Item> items) {

    public record Item(
            @Schema(description = "활동 발생 시각") Instant at,
            @Schema(description = "활동 종류") ActivityKind kind,
            @Schema(description = "타임라인 한 줄 요약") String summary,
            @Schema(description = "추억 답변인 경우 대상 추억 id, 그 외 null") UUID memoryId
    ) {}

    public static TodayActivitiesResponse from(List<ActivityEntry> entries) {
        return new TodayActivitiesResponse(entries.stream()
                .map(e -> new Item(e.at(), e.kind(), e.summary(), e.memoryId()))
                .toList());
    }
}
