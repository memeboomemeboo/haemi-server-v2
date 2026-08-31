package com.memeboo2.haemi.guardian.dailycare.presentation.dto;

import com.memeboo2.haemi.guardian.dailycare.domain.CareType;
import com.memeboo2.haemi.guardian.dailycare.domain.DailyCare;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;

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
        return from(c, null);
    }

    public static SentDailyCareItem from(DailyCare c, MediaUploadCommand mediaUploadCommand) {
        return new SentDailyCareItem(
                c.getId(), c.getCareDate(), c.getCareType(), c.getText(),
                mediaUploadCommand == null ? c.getMediaKey() : mediaUploadCommand.resolveServingUrl(c.getMediaKey()),
                c.getDurationSeconds(), c.isRead()
        );
    }
}
