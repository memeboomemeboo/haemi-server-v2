package com.memeboo2.haemi.guardian.dailycare.presentation.dto;

import com.memeboo2.haemi.guardian.dailycare.domain.CareType;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;

import java.time.LocalDate;
import java.util.UUID;

public record SentDailyCareItem(
        UUID id,
        LocalDate careDate,
        CareType type,
        String text,
        String mediaKey,
        Integer durationSeconds,
        boolean read
) {
    public static SentDailyCareItem from(DailyCare c) {
        return new SentDailyCareItem(
                c.getId(), c.getCareDate(), c.getCareType(), c.getText(),
                c.getMediaKey(), c.getDurationSeconds(), c.isRead()
        );
    }
}
