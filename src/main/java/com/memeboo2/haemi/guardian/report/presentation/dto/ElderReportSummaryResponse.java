package com.memeboo2.haemi.guardian.report.presentation.dto;

import com.memeboo2.haemi.guardian.report.application.GetElderReportSummaryUseCase.Summary;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record ElderReportSummaryResponse(
        UUID elderId,
        String name,
        Integer age,
        String generation,
        @Schema(description = "첫 등록일부터 지난 일수")
        long daysTogether,
        boolean attendedToday,
        @Schema(description = "최근 7일 중 참여한 일수")
        int weeklyParticipationDays,
        int weeklyGoalDays,
        @Schema(description = "3색 종합상태. 수치 점수는 노출하지 않는다 (D11)")
        ReportStatus status,
        int currentStreak,
        int bestStreak
) {
    public static ElderReportSummaryResponse from(Summary s) {
        return new ElderReportSummaryResponse(
                s.elderId(), s.name(), s.age(), s.generation(), s.daysTogether(), s.attendedToday(),
                s.weeklyParticipationDays(), s.weeklyGoalDays(), s.status(), s.currentStreak(), s.bestStreak()
        );
    }
}
