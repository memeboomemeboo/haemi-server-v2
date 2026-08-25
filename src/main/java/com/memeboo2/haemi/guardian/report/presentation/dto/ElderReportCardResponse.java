package com.memeboo2.haemi.guardian.report.presentation.dto;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.report.application.GetElderReportListUseCase.Card;
import com.memeboo2.haemi.guardian.report.domain.ReportStatus;

import java.util.UUID;

public record ElderReportCardResponse(
        UUID elderId,
        String name,
        GuardianRole role,
        String roleLabel,
        Integer age,
        boolean attendedToday,
        ReportStatus status
) {
    public static ElderReportCardResponse from(Card c) {
        return new ElderReportCardResponse(
                c.elderId(), c.name(), c.role(), c.role().getLabel(), c.age(), c.attendedToday(), c.status());
    }
}
